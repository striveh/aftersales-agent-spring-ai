package dev.striveh.aftersales.agent.api;

import java.math.RoundingMode;
import java.util.List;

import dev.striveh.aftersales.agent.answer.AnswerComposer;
import dev.striveh.aftersales.agent.plan.AgentTurn;
import dev.striveh.aftersales.agent.runtime.AgentRuntime;
import dev.striveh.aftersales.agent.tool.Approval;
import dev.striveh.aftersales.agent.tool.ApprovalService;
import dev.striveh.aftersales.agent.tool.ApprovalStatus;
import dev.striveh.aftersales.common.DemoCustomer;
import dev.striveh.aftersales.common.OrderCatalog;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentRuntime runtime;

    public ChatController(AgentRuntime runtime) {
        this.runtime = runtime;
    }

    @PostMapping("/chat")
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest request) {
        AgentTurn turn = runtime.agent().handle(request.conversationId(), request.customerPhone(), request.message().trim());
        return toDto(turn);
    }

    @PostMapping("/approvals/{id}")
    public ChatResponseDto decide(@PathVariable String id, @RequestBody ApprovalRequest request) {
        Approval current = runtime.approvals().find(id)
                .orElseThrow(() -> new ApprovalService.ApprovalNotFoundException(id));
        if (current.status() != ApprovalStatus.PENDING) {
            return approvalReply(current, "这笔申请已经处理过，不会重复入账。");
        }
        Approval updated = runtime.approvals().decide(id, request.approved());
        String answer = updated.status() == ApprovalStatus.APPROVED
                ? "退款已提交人工审核，金额 " + AnswerComposer.yuan(updated.amount()) + "。演示账本记下了这一笔，不会真实出账。"
                : "已拒绝这笔退款，账本没有入账。";
        runtime.traces().add(updated.conversationId(), "APPROVAL", "request_refund", updated.status().name(), answer);
        return approvalReply(updated, answer);
    }

    @GetMapping("/traces/{conversationId}")
    public List<dev.striveh.aftersales.agent.trace.TraceEvent> trace(@PathVariable String conversationId) {
        return runtime.traces().get(conversationId);
    }

    @GetMapping("/demo/customers")
    public List<DemoCustomer> customers() {
        return new OrderCatalog().customers();
    }

    private ChatResponseDto approvalReply(Approval approval, String answer) {
        return new ChatResponseDto(approval.conversationId(), answer, approval.status().name(), false, approval.orderId(),
                List.of(), view(approval), runtime.traces().get(approval.conversationId()), runtime.mode());
    }

    private ChatResponseDto toDto(AgentTurn turn) {
        return new ChatResponseDto(
                turn.conversationId(),
                turn.answer(),
                turn.decision().name(),
                turn.refundable(),
                turn.orderId(),
                turn.citations(),
                view(turn.pendingApproval()),
                turn.trace(),
                turn.mode());
    }

    private ApprovalView view(Approval approval) {
        if (approval == null) {
            return null;
        }
        return new ApprovalView(approval.id(), approval.orderId(), approval.customerName(),
                approval.amount().setScale(2, RoundingMode.HALF_UP), approval.reason(), approval.status().name());
    }
}
