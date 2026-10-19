package dev.striveh.aftersales.agent.eligibility;

import java.time.LocalDate;
import java.util.List;

import dev.striveh.aftersales.common.Order;

public record DecisionFacts(
        Intent intent,
        String message,
        Order order,
        List<Order> candidates,
        LocalDate today,
        boolean lookupFailed
) {
}
