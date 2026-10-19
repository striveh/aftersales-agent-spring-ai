package dev.striveh.aftersales.agent.tool;

import dev.striveh.aftersales.agent.eligibility.Intent;
import dev.striveh.aftersales.common.Order;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Sensitive tool. Calling it never credits the ledger. It only opens a pending
 * approval, and only after the current turn has already verified the order.
 */
public final class RefundTool {

    private final ApprovalService approvals;
    private final ThreadLocal<Turn> turn = new ThreadLocal<>();

    public RefundTool(ApprovalService approvals) {
        this.approvals = approvals;
    }

    public void bind(Turn current) {
        turn.set(current);
    }

    public void clear() {
        turn.remove();
    }

    @Tool(description = "提交退款申请。只生成待人工确认的单据，不会打款。金额以已核实订单的实付为准。")
    public String requestRefund(
            @ToolParam(description = "已核实的订单号") String orderId,
            @ToolParam(description = "退款原因") String reason) {
        Turn current = turn.get();
        if (current == null || current.order() == null) {
            return status("REJECTED", "没有已核实的订单，拒绝退款", null);
        }
        if (orderId != null && !orderId.isBlank() && !orderId.startsWith("$") && !current.order().orderId().equals(orderId)) {
            return status("REJECTED", "订单号与已核实订单不一致", null);
        }
        if (current.intent() != Intent.REFUND) {
            return status("SKIPPED", "用户没有明确要求发起退款", null);
        }
        if (!current.refundable()) {
            return status("SKIPPED", "资格不允许退款", null);
        }
        Approval approval = approvals.open(current.conversationId(), current.order(),
                reason == null || reason.isBlank() ? current.reason() : reason);
        return status("PENDING", "已生成待确认退款，尚未打款", approval.id());
    }

    private String status(String status, String message, String approvalId) {
        String id = approvalId == null ? "" : approvalId;
        return "{\"status\":\"" + status + "\",\"message\":\"" + message + "\",\"approvalId\":\"" + id + "\"}";
    }

    public record Turn(String conversationId, Order order, Intent intent, boolean refundable, String reason) {
    }
}
