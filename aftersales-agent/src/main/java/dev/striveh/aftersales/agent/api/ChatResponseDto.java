package dev.striveh.aftersales.agent.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import dev.striveh.aftersales.agent.answer.Citation;
import dev.striveh.aftersales.agent.trace.TraceEvent;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponseDto(
        String conversationId,
        String answer,
        String decision,
        boolean refundable,
        String orderId,
        List<Citation> citations,
        ApprovalView pendingApproval,
        List<TraceEvent> trace,
        String mode
) {
}
