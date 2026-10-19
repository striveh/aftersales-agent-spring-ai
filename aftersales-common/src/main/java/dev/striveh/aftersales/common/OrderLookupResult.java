package dev.striveh.aftersales.common;

import java.util.List;

public record OrderLookupResult(String status, String message, List<Order> orders) {

    public static OrderLookupResult ok(List<Order> orders) {
        return new OrderLookupResult("OK", "找到 " + orders.size() + " 笔订单", List.copyOf(orders));
    }

    public static OrderLookupResult ambiguous(List<Order> orders) {
        return new OrderLookupResult("AMBIGUOUS", "匹配到多笔订单，需要用户指定", List.copyOf(orders));
    }

    public static OrderLookupResult notFound(String message) {
        return new OrderLookupResult("NOT_FOUND", message, List.of());
    }

    public static OrderLookupResult badRequest(String message) {
        return new OrderLookupResult("BAD_REQUEST", message, List.of());
    }

    public boolean foundOne() {
        return "OK".equals(status) && orders.size() == 1;
    }
}
