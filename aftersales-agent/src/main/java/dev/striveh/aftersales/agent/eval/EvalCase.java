package dev.striveh.aftersales.agent.eval;

import java.util.List;

public record EvalCase(String id, String title, String customerPhone, boolean approveAtEnd, List<EvalTurn> turns) {
}
