package dev.striveh.aftersales.agent.eval;

import java.util.List;

public record EvalReport(int total, List<String> failures, String summary) {

    public boolean passed() {
        return failures.isEmpty();
    }
}
