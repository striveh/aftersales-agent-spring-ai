package dev.striveh.aftersales.agent.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class PlanParser {

    private final ObjectMapper json;

    public PlanParser(ObjectMapper json) {
        this.json = json;
    }

    public Plan parse(String text) {
        String body = extractObject(text);
        try {
            JsonNode root = json.readTree(body);
            String reason = root.path("reason").asText("");
            List<PlanStep> steps = new ArrayList<>();
            for (JsonNode node : root.withArray("steps")) {
                String action = node.path("action").asText("").trim();
                if (action.isBlank()) {
                    continue;
                }
                AgentAction.fromWire(action);
                Map<String, Object> args = new LinkedHashMap<>();
                JsonNode argsNode = node.path("args");
                if (argsNode.isObject()) {
                    argsNode.fields().forEachRemaining(entry -> args.put(entry.getKey(), entry.getValue().asText("")));
                }
                steps.add(new PlanStep(node.path("id").asText("s" + (steps.size() + 1)), action, args,
                        node.path("purpose").asText("")));
            }
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("计划里没有步骤");
            }
            return new Plan(reason, steps);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("无法解析计划 JSON", exception);
        }
    }

    static String extractObject(String text) {
        if (text == null) {
            throw new IllegalArgumentException("空计划");
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            }
        }
        int left = trimmed.indexOf('{');
        int right = trimmed.lastIndexOf('}');
        if (left < 0 || right <= left) {
            throw new IllegalArgumentException("回复里没有 JSON 对象");
        }
        return trimmed.substring(left, right + 1);
    }
}
