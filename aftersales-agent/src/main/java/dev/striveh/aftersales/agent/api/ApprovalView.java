package dev.striveh.aftersales.agent.api;

import java.math.BigDecimal;

public record ApprovalView(String id, String orderId, String customerName, BigDecimal amount, String reason, String status) {
}
