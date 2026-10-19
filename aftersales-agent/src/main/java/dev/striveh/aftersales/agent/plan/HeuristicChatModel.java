package dev.striveh.aftersales.agent.plan;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/**
 * Offline stand-in for an OpenAI-compatible chat model. It only understands the
 * labeled planning prompt; answer text is composed in Java.
 */
public final class HeuristicChatModel implements ChatModel {

    private final HeuristicPlans plans;
    private final ObjectMapper json;

    public HeuristicChatModel(HeuristicPlans plans, ObjectMapper json) {
        this.plans = plans;
        this.json = json;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String text = prompt.getInstructions().stream().map(Message::getText).reduce("", (left, right) -> left + "\n" + right);
        String task = field(text, "TASK");
        PlanContext context = new PlanContext(
                field(text, "USER_MESSAGE"),
                field(text, "CUSTOMER_PHONE"),
                field(text, "KNOWN_ORDER_ID"),
                "true".equalsIgnoreCase(field(text, "PREVIOUS_REFUNDABLE")),
                "true".equalsIgnoreCase(field(text, "IGNORE_ORDER_ID")));
        Plan plan = "REPLAN".equals(task)
                ? plans.replan(context, field(text, "FAILED_ACTION"), field(text, "FAILURE_MESSAGE"))
                : plans.plan(context);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(write(plan)))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().build();
    }

    private String write(Plan plan) {
        try {
            return json.writeValueAsString(plan);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String field(String prompt, String name) {
        String prefix = name + ":";
        for (String line : prompt.split("\\R")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }
}
