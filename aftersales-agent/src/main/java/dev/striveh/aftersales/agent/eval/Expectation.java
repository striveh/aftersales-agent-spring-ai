package dev.striveh.aftersales.agent.eval;

import java.util.List;

public record Expectation(
        String decision,
        List<String> requiredActions,
        List<String> forbiddenActions,
        String orderId,
        Boolean refundable,
        Boolean awaitingApproval,
        Boolean refundExecuted,
        Boolean replanned,
        String citationDoc,
        String citationSectionContains,
        List<String> answerContains
) {
}
