package dev.striveh.aftersales.agent.plan;

import java.util.Map;

public record PlanStep(String id, String action, Map<String, Object> args, String purpose) {

    public PlanStep {
        args = args == null ? Map.of() : Map.copyOf(args);
        purpose = purpose == null ? "" : purpose;
    }
}
