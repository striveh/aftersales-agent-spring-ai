package dev.striveh.aftersales.agent.plan;

import java.util.List;

public record Plan(String reason, List<PlanStep> steps) {

    public Plan {
        reason = reason == null ? "" : reason;
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public boolean sameSteps(Plan other) {
        if (other == null || steps.size() != other.steps.size()) {
            return false;
        }
        for (int i = 0; i < steps.size(); i++) {
            PlanStep left = steps.get(i);
            PlanStep right = other.steps.get(i);
            if (!left.action().equals(right.action()) || !left.args().equals(right.args())) {
                return false;
            }
        }
        return true;
    }
}
