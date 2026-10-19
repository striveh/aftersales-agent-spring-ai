package dev.striveh.aftersales.agent.plan;

import java.util.Map;

public record StepResult(StepStatus status, String detail, Map<String, String> output) {

    public StepResult {
        detail = detail == null ? "" : detail;
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static StepResult ok(String detail, Map<String, String> output) {
        return new StepResult(StepStatus.OK, detail, output);
    }

    public static StepResult failed(String detail) {
        return new StepResult(StepStatus.FAILED, detail, Map.of());
    }

    public static StepResult skipped(String detail) {
        return new StepResult(StepStatus.SKIPPED, detail, Map.of());
    }

    public static StepResult awaiting(String detail, String approvalId) {
        return new StepResult(StepStatus.AWAITING_APPROVAL, detail, Map.of("approvalId", approvalId));
    }
}
