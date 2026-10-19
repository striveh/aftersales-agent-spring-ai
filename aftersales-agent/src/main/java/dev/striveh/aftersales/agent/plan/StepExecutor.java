package dev.striveh.aftersales.agent.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.agent.answer.AnswerComposer;
import dev.striveh.aftersales.agent.eligibility.DecisionFacts;
import dev.striveh.aftersales.agent.eligibility.EligibilityDecider;
import dev.striveh.aftersales.agent.eligibility.Intent;
import dev.striveh.aftersales.agent.retrieval.HybridPolicyRetriever;
import dev.striveh.aftersales.agent.retrieval.PolicyChunk;
import dev.striveh.aftersales.agent.tool.ApprovalService;
import dev.striveh.aftersales.agent.tool.OrderGateway;
import dev.striveh.aftersales.agent.tool.RefundTool;
import dev.striveh.aftersales.common.Order;
import dev.striveh.aftersales.common.OrderLookupResult;
import dev.striveh.aftersales.common.LogisticsLookupResult;

public final class StepExecutor {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$([A-Za-z0-9_]+)\\.([A-Za-z0-9_]+)");

    private final OrderGateway orders;
    private final HybridPolicyRetriever retriever;
    private final EligibilityDecider decider;
    private final AnswerComposer composer;
    private final RefundTool refundTool;
    private final ApprovalService approvals;
    private final ObjectMapper json;
    private final LocalDate today;

    public StepExecutor(OrderGateway orders, HybridPolicyRetriever retriever, EligibilityDecider decider,
                        AnswerComposer composer, RefundTool refundTool, ApprovalService approvals, ObjectMapper json,
                        LocalDate today) {
        this.orders = orders;
        this.retriever = retriever;
        this.decider = decider;
        this.composer = composer;
        this.refundTool = refundTool;
        this.approvals = approvals;
        this.json = json;
        this.today = today;
    }

    public StepResult execute(PlanStep step, ExecutionState state) {
        state.action(step.action());
        AgentAction action = AgentAction.fromWire(step.action());
        return switch (action) {
            case LOOKUP_ORDER -> lookupOrder(step, state);
            case LOOKUP_LOGISTICS -> lookupLogistics(step, state);
            case RETRIEVE_POLICY -> retrieve(step, state);
            case DECIDE_ELIGIBILITY -> decide(step, state);
            case REQUEST_REFUND -> refund(state);
            case DRAFT_ANSWER -> draft(state);
        };
    }

    private StepResult lookupOrder(PlanStep step, ExecutionState state) {
        String orderId = arg(step, "orderId", state);
        String phone = first(arg(step, "customerPhone", state), state.customerPhone());
        OrderLookupResult result = orders.lookup(orderId, phone, arg(step, "productKeyword", state), arg(step, "timeHint", state));
        if ("NOT_FOUND".equals(result.status()) || "BAD_REQUEST".equals(result.status())) {
            state.lookupFailed(true);
            state.order(null);
            return StepResult.failed(result.message());
        }
        state.lookupFailed(false);
        if ("AMBIGUOUS".equals(result.status())) {
            state.order(null);
            state.candidates(result.orders());
            state.remember(step.id(), Map.of("status", "AMBIGUOUS", "matchCount", Integer.toString(result.orders().size())));
            return StepResult.ok("匹配到 " + result.orders().size() + " 笔订单", Map.of("status", "AMBIGUOUS"));
        }
        Order order = result.orders().get(0);
        state.order(order);
        state.candidates(List.of(order));
        Map<String, String> output = Map.of("orderId", order.orderId(), "status", "OK", "productName", order.productName());
        state.remember(step.id(), output);
        return StepResult.ok("找到订单 " + order.orderId(), output);
    }

    private StepResult lookupLogistics(PlanStep step, ExecutionState state) {
        String orderId = arg(step, "orderId", state);
        if (orderId.isBlank() && state.order() != null) {
            orderId = state.order().orderId();
        }
        if (orderId.isBlank() && state.candidates().size() > 1) {
            return StepResult.skipped("有多笔订单，先澄清，暂不查物流");
        }
        if (orderId.isBlank()) {
            return StepResult.failed("没有订单号，无法查物流");
        }
        LogisticsLookupResult result = orders.logistics(orderId);
        if (!"OK".equals(result.status()) || result.logistics() == null) {
            return StepResult.failed(result.message());
        }
        state.logistics(result.logistics());
        Map<String, String> output = Map.of("orderId", orderId, "statusText", result.logistics().statusText());
        state.remember(step.id(), output);
        return StepResult.ok(result.logistics().statusText(), output);
    }

    private StepResult retrieve(PlanStep step, ExecutionState state) {
        String query = (state.message() + " " + arg(step, "query", state)).trim();
        List<PolicyChunk> clauses = retriever.retrieve(query);
        state.clauses(clauses);
        String titles = clauses.stream().map(PolicyChunk::sectionTitle).reduce((left, right) -> left + " | " + right).orElse("无");
        return StepResult.ok("检索到 " + clauses.size() + " 条：" + titles, Map.of("count", Integer.toString(clauses.size())));
    }

    private StepResult decide(PlanStep step, ExecutionState state) {
        var decision = decider.decide(facts(state));
        state.decision(decision);
        if (decision.order() != null) {
            state.order(decision.order());
        }
        state.remember(step.id(), Map.of("decision", decision.code().name(), "refundable", Boolean.toString(decision.refundable())));
        return StepResult.ok(decision.code() + " " + decision.summary(), Map.of("decision", decision.code().name()));
    }

    private StepResult refund(ExecutionState state) {
        refundTool.bind(new RefundTool.Turn(
                state.conversationId(),
                state.order(),
                state.intent(),
                state.decision() != null && state.decision().refundable(),
                state.decision() == null ? "" : state.decision().summary()));
        try {
            String orderId = state.order() == null ? "" : state.order().orderId();
            String raw = refundTool.requestRefund(orderId, state.decision() == null ? "" : state.decision().summary());
            JsonNode node = json.readTree(raw);
            String status = node.path("status").asText();
            String message = node.path("message").asText();
            if ("PENDING".equals(status)) {
                String approvalId = node.path("approvalId").asText();
                approvals.find(approvalId).ifPresent(state::pending);
                return StepResult.awaiting(message, approvalId);
            }
            if ("SKIPPED".equals(status)) {
                return StepResult.skipped(message);
            }
            return StepResult.failed(message);
        } catch (Exception exception) {
            return StepResult.failed("退款工具失败：" + exception.getMessage());
        } finally {
            refundTool.clear();
        }
    }

    private StepResult draft(ExecutionState state) {
        if (state.decision() == null) {
            state.decision(decider.decide(facts(state)));
        }
        state.answer(composer.compose(state, today));
        return StepResult.ok("已生成答复", Map.of());
    }

    private DecisionFacts facts(ExecutionState state) {
        boolean failed = state.lookupFailed() && state.order() == null
                && state.intent() != Intent.POLICY
                && state.intent() != Intent.SMALL_TALK
                && state.intent() != Intent.OUT_OF_SCOPE;
        return new DecisionFacts(state.intent(), state.message(), state.order(), state.candidates(), today, failed);
    }

    private String arg(PlanStep step, String name, ExecutionState state) {
        Object value = step.args().get(name);
        return resolve(value == null ? "" : String.valueOf(value), state);
    }

    private String resolve(String value, ExecutionState state) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String replacement = state.output(matcher.group(1)).getOrDefault(matcher.group(2), "");
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString().trim();
    }

    private static String first(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
