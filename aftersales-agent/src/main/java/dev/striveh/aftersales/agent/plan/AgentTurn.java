package dev.striveh.aftersales.agent.plan;

import java.util.List;

import dev.striveh.aftersales.agent.answer.Citation;
import dev.striveh.aftersales.agent.eligibility.DecisionCode;
import dev.striveh.aftersales.agent.tool.Approval;
import dev.striveh.aftersales.agent.trace.TraceEvent;

public record AgentTurn(
        String conversationId,
        String answer,
        DecisionCode decision,
        boolean refundable,
        String orderId,
        List<Citation> citations,
        Approval pendingApproval,
        List<TraceEvent> trace,
        boolean replanned,
        boolean planFellBack,
        String mode
) {
}
