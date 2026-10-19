package dev.striveh.aftersales.agent.tool;

import java.math.BigDecimal;
import java.time.Instant;

public record Approval(
        String id,
        String conversationId,
        String orderId,
        String customerName,
        BigDecimal amount,
        String reason,
        ApprovalStatus status,
        Instant createdAt
) {
}
