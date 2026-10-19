package dev.striveh.aftersales.common;

import java.time.LocalDate;
import java.util.List;

public record Logistics(
        String orderId,
        String carrier,
        String trackingNo,
        String statusText,
        LocalDate signedOn,
        List<LogisticsEvent> events
) {
}
