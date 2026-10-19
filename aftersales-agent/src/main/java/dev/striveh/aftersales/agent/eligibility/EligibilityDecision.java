package dev.striveh.aftersales.agent.eligibility;

import dev.striveh.aftersales.common.Order;

public record EligibilityDecision(
        DecisionCode code,
        boolean refundable,
        String docId,
        String sectionTitle,
        String summary,
        Order order
) {

    public static EligibilityDecision of(DecisionCode code, boolean refundable, String docId, String sectionTitle,
                                         String summary, Order order) {
        return new EligibilityDecision(code, refundable, docId, sectionTitle, summary, order);
    }
}
