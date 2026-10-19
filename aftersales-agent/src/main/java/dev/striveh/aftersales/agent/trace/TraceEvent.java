package dev.striveh.aftersales.agent.trace;

import java.time.Instant;

public record TraceEvent(String type, String action, String status, String detail, Instant at) {
}
