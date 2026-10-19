package dev.striveh.aftersales.eval;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import dev.striveh.aftersales.agent.eval.EvalCase;
import dev.striveh.aftersales.agent.eval.EvalHarness;
import dev.striveh.aftersales.agent.eval.EvalReport;
import dev.striveh.aftersales.agent.eval.LlmJudge;
import dev.striveh.aftersales.agent.plan.AgentTurn;
import dev.striveh.aftersales.agent.plan.HeuristicPlans;
import dev.striveh.aftersales.agent.retrieval.HashingEmbeddingModel;
import dev.striveh.aftersales.agent.retrieval.InMemoryPolicyIndex;
import dev.striveh.aftersales.agent.runtime.AgentRuntime;
import dev.striveh.aftersales.agent.runtime.JsonSupport;
import dev.striveh.aftersales.agent.runtime.OpenAiChatModels;
import dev.striveh.aftersales.agent.tool.LocalOrderGateway;

import org.springframework.ai.chat.model.ChatModel;

/**
 * Deterministic eval always runs. {@code --live} also sends planning and answer
 * polishing to the configured OpenAI-compatible model, then asks that model to
 * judge the prose. Structural checks still come from the code path.
 */
public final class EvalApplication {

    private EvalApplication() {
    }

    public static void main(String[] args) {
        List<String> arguments = Arrays.asList(args);
        boolean live = arguments.contains("--live") || "true".equalsIgnoreCase(System.getenv("EVAL_JUDGE"));
        List<EvalCase> cases = EvalHarness.loadCases();
        AgentRuntime runtime = live ? liveRuntime() : AgentRuntime.offline(EvalHarness.TODAY).ingestClasspath();
        if (runtime == null) {
            System.exit(2);
            return;
        }
        EvalReport report = new EvalHarness(runtime, cases).run();
        System.out.println(report.summary());
        report.failures().forEach(failure -> System.out.println("FAIL " + failure));
        if (live) {
            judge(runtime, cases);
        }
        System.exit(report.passed() ? 0 : 1);
    }

    private static AgentRuntime liveRuntime() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("OPENAI_API_KEY 未设置，跳过在线评测。");
            return null;
        }
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "deepseek-chat");
        ChatModel chatModel = OpenAiChatModels.create(baseUrl, apiKey, model, 0.2);
        Clock clock = Clock.fixed(EvalHarness.TODAY.atStartOfDay(AgentRuntime.ZONE).toInstant(), AgentRuntime.ZONE);
        System.out.println("在线评测 model=" + model + " baseUrl=" + baseUrl);
        return AgentRuntime.create(clock, new InMemoryPolicyIndex(new HashingEmbeddingModel()),
                        new LocalOrderGateway(EvalHarness.TODAY), chatModel, new HeuristicPlans(), false, 8, 4,
                        "openai", false, JsonSupport.mapper())
                .ingestClasspath();
    }

    private static void judge(AgentRuntime runtime, List<EvalCase> cases) {
        LlmJudge judge = new LlmJudge(OpenAiChatModels.create(
                System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.deepseek.com"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv().getOrDefault("OPENAI_MODEL", "deepseek-chat"),
                0.0));
        System.out.println("LLM judge（分数来自模型，不写入仓库作为既成指标）");
        for (EvalCase evalCase : cases) {
            AgentTurn turn = runtime.agent().handle("judge-" + evalCase.id(), evalCase.customerPhone(),
                    evalCase.turns().get(evalCase.turns().size() - 1).message());
            System.out.println(evalCase.id() + " " + judge.score(evalCase, turn));
        }
    }
}
