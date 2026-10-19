package dev.striveh.aftersales.agent.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import dev.striveh.aftersales.agent.eligibility.DecisionCode;
import dev.striveh.aftersales.agent.retrieval.HashingEmbeddingModel;
import dev.striveh.aftersales.agent.retrieval.InMemoryPolicyIndex;
import dev.striveh.aftersales.agent.runtime.AgentRuntime;
import dev.striveh.aftersales.agent.runtime.JsonSupport;
import dev.striveh.aftersales.agent.tool.LocalOrderGateway;
import dev.striveh.aftersales.agent.tool.RefundTool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

class PlanGuardTest {

    @Test
    void identicalFailedPlanStopsInsteadOfLooping() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        Clock clock = Clock.fixed(today.atStartOfDay(AgentRuntime.ZONE).toInstant(), AgentRuntime.ZONE);
        ChatModel sticky = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                String json = """
                        {"reason":"bad","steps":[{"id":"s1","action":"lookup_order","args":{"orderId":"ORD-NOPE","customerPhone":"13800001001","productKeyword":"耳机","timeHint":""},"purpose":"查"}]}
                        """;
                return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }
        };
        AgentRuntime runtime = AgentRuntime.create(clock, new InMemoryPolicyIndex(new HashingEmbeddingModel()),
                new LocalOrderGateway(today), sticky, new HeuristicPlans(), false, 8, 4, "scripted", true,
                JsonSupport.mapper()).ingestClasspath();
        AgentTurn turn = runtime.agent().handle("guard", "13800001001", "这单耳机能退吗");
        assertThat(turn.decision()).isEqualTo(DecisionCode.LOOKUP_FAILED);
        assertThat(turn.replanned()).isFalse();
    }

    @Test
    void refundToolRefusesWithoutAVerifiedTurn() {
        RefundTool tool = new RefundTool(null);
        assertThat(tool.requestRefund("ORD-DEMO-1001", "测试")).contains("REJECTED");
    }

    @Test
    void garbagePlanFallsBackWhenAllowed() {
        AgentRuntime runtime = AgentRuntime.offline(LocalDate.of(2026, 10, 19)).ingestClasspath();
        AgentTurn turn = runtime.agent().handle(null, "13800001001", "我上周买的耳机能退吗");
        assertThat(turn.decision()).isEqualTo(DecisionCode.RETURN_NO_REASON);
        assertThat(turn.planFellBack()).isFalse();
        assertThat(turn.citations()).isNotEmpty();
    }
}
