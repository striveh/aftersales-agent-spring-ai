package dev.striveh.aftersales.agent.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(String conversationId, @NotBlank String message, String customerPhone) {
}
