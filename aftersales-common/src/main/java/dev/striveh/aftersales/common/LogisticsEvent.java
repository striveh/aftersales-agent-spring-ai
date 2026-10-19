package dev.striveh.aftersales.common;

import java.time.LocalDate;

public record LogisticsEvent(LocalDate date, String description) {
}
