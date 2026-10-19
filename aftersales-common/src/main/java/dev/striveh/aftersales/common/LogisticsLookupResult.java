package dev.striveh.aftersales.common;

public record LogisticsLookupResult(String status, String message, Logistics logistics) {

    public static LogisticsLookupResult ok(Logistics logistics) {
        return new LogisticsLookupResult("OK", "已读取物流轨迹", logistics);
    }

    public static LogisticsLookupResult notFound(String message) {
        return new LogisticsLookupResult("NOT_FOUND", message, null);
    }

    public static LogisticsLookupResult badRequest(String message) {
        return new LogisticsLookupResult("BAD_REQUEST", message, null);
    }
}
