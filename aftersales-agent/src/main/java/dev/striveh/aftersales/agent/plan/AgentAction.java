package dev.striveh.aftersales.agent.plan;

import java.util.Arrays;

public enum AgentAction {
    LOOKUP_ORDER("lookup_order"),
    LOOKUP_LOGISTICS("lookup_logistics"),
    RETRIEVE_POLICY("retrieve_policy"),
    DECIDE_ELIGIBILITY("decide_eligibility"),
    REQUEST_REFUND("request_refund"),
    DRAFT_ANSWER("draft_answer");

    private final String wire;

    AgentAction(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static AgentAction fromWire(String wire) {
        return Arrays.stream(values())
                .filter(action -> action.wire.equals(wire))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知动作 " + wire));
    }
}
