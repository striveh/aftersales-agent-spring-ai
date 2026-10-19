package dev.striveh.aftersales.agent.eval;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.agent.answer.Citation;
import dev.striveh.aftersales.agent.plan.AgentTurn;
import dev.striveh.aftersales.agent.runtime.AgentRuntime;
import dev.striveh.aftersales.agent.runtime.JsonSupport;
import dev.striveh.aftersales.agent.tool.Approval;
import dev.striveh.aftersales.agent.tool.ApprovalStatus;
import dev.striveh.aftersales.agent.trace.TraceEvent;

import org.springframework.ai.chat.model.ChatModel;

public final class EvalHarness {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    private final AgentRuntime runtime;
    private final List<EvalCase> cases;
    private final boolean checkAnswerText;

    public EvalHarness(AgentRuntime runtime, List<EvalCase> cases) {
        this.runtime = runtime;
        this.cases = cases;
        this.checkAnswerText = runtime.heuristic();
    }

    public static List<EvalCase> loadCases() {
        ObjectMapper json = JsonSupport.mapper();
        try (InputStream input = EvalHarness.class.getResourceAsStream("/eval/cases.json")) {
            if (input == null) {
                throw new IllegalStateException("缺少 eval/cases.json");
            }
            return json.readValue(input, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取评测集", exception);
        }
    }

    public static EvalReport offline() {
        AgentRuntime runtime = AgentRuntime.offline(TODAY).ingestClasspath();
        return new EvalHarness(runtime, loadCases()).run();
    }

    public EvalReport run() {
        List<String> failures = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            failures.addAll(runCase(evalCase));
        }
        String summary = "确定性评测 " + (cases.size() - failures.size()) + "/" + cases.size()
                + " 通过，失败 " + failures.size() + "。模式=" + runtime.mode() + "。";
        return new EvalReport(cases.size(), List.copyOf(failures), summary);
    }

    private List<String> runCase(EvalCase evalCase) {
        List<String> failures = new ArrayList<>();
        String conversationId = "eval-" + evalCase.id();
        AgentTurn last = null;
        int turnIndex = 0;
        for (EvalTurn turn : evalCase.turns()) {
            turnIndex++;
            AgentTurn result = runtime.agent().handle(conversationId, evalCase.customerPhone(), turn.message());
            last = result;
            check(failures, evalCase, turnIndex, turn.expect(), result);
        }
        if (evalCase.approveAtEnd()) {
            if (last == null || last.pendingApproval() == null) {
                failures.add(evalCase.id() + " 结束时没有待确认的退款");
            } else {
                Approval approval = runtime.approvals().decide(last.pendingApproval().id(), true);
                if (approval.status() != ApprovalStatus.APPROVED || !runtime.ledger().exists(approval.orderId())) {
                    failures.add(evalCase.id() + " 确认后没有入账");
                }
                int size = runtime.ledger().size();
                runtime.approvals().decide(approval.id(), true);
                if (runtime.ledger().size() != size) {
                    failures.add(evalCase.id() + " 重复确认产生了第二笔退款");
                }
            }
        }
        return failures;
    }

    private void check(List<String> failures, EvalCase evalCase, int turnIndex, Expectation expect, AgentTurn result) {
        String where = evalCase.id() + " turn " + turnIndex;
        List<String> actions = result.trace().stream()
                .filter(event -> "STEP".equals(event.type()))
                .map(TraceEvent::action)
                .toList();
        if (expect.decision() != null && !expect.decision().equals(result.decision().name())) {
            failures.add(where + " decision 期望 " + expect.decision() + " 实际 " + result.decision()
                    + " 回答=" + result.answer());
        }
        if (expect.requiredActions() != null && !subsequence(actions, expect.requiredActions())) {
            failures.add(where + " 工具序列缺少 " + expect.requiredActions() + " 实际 " + actions);
        }
        if (expect.forbiddenActions() != null) {
            for (String forbidden : expect.forbiddenActions()) {
                if (actions.contains(forbidden)) {
                    failures.add(where + " 不应调用 " + forbidden + " 实际 " + actions);
                }
            }
        }
        if (expect.orderId() != null && !expect.orderId().equals(result.orderId())) {
            failures.add(where + " orderId 期望 " + expect.orderId() + " 实际 " + result.orderId());
        }
        if (expect.refundable() != null && expect.refundable() != result.refundable()) {
            failures.add(where + " refundable 期望 " + expect.refundable() + " 实际 " + result.refundable());
        }
        boolean awaiting = result.pendingApproval() != null;
        if (expect.awaitingApproval() != null && expect.awaitingApproval() != awaiting) {
            failures.add(where + " awaitingApproval 期望 " + expect.awaitingApproval() + " 实际 " + awaiting);
        }
        if (expect.replanned() != null && expect.replanned() != result.replanned()) {
            failures.add(where + " replanned 期望 " + expect.replanned() + " 实际 " + result.replanned());
        }
        if (expect.refundExecuted() != null) {
            boolean executed = result.orderId() != null && runtime.ledger().exists(result.orderId());
            if (expect.refundExecuted() != executed) {
                failures.add(where + " refundExecuted 期望 " + expect.refundExecuted() + " 实际 " + executed);
            }
        }
        if (expect.citationDoc() != null || expect.citationSectionContains() != null) {
            boolean cited = result.citations().stream().anyMatch(citation -> citationMatches(citation, expect));
            if (!cited) {
                failures.add(where + " 缺少引用 " + expect.citationDoc() + " / " + expect.citationSectionContains()
                        + " 实际 " + result.citations());
            }
        }
        if (checkAnswerText && expect.answerContains() != null) {
            for (String snippet : expect.answerContains()) {
                if (result.answer() == null || !result.answer().contains(snippet)) {
                    failures.add(where + " 回答缺少「" + snippet + "」实际=" + result.answer());
                }
            }
        }
        if (!runtime.heuristic() && result.planFellBack()) {
            failures.add(where + " 在线规划解析失败并回退到了规则规划器");
        }
    }

    private boolean citationMatches(Citation citation, Expectation expect) {
        if (expect.citationDoc() != null && !expect.citationDoc().equals(citation.docId())) {
            return false;
        }
        return expect.citationSectionContains() == null || citation.sectionTitle().contains(expect.citationSectionContains());
    }

    static boolean subsequence(List<String> actual, List<String> required) {
        int cursor = 0;
        for (String action : actual) {
            if (cursor < required.size() && action.equals(required.get(cursor))) {
                cursor++;
            }
        }
        return cursor == required.size();
    }

    public static String judge(ChatModel model, EvalCase evalCase, AgentTurn turn) {
        return new LlmJudge(model).score(evalCase, turn);
    }
}
