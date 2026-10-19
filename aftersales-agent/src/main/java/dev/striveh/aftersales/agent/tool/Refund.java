package dev.striveh.aftersales.agent.tool;

import java.math.BigDecimal;
import java.time.Instant;

public record Refund(String approvalId, String orderId, BigDecimal amount, Instant executedAt) {
}
