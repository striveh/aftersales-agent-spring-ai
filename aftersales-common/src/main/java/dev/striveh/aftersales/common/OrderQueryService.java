package dev.striveh.aftersales.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only order and logistics queries. The MCP server and the in-process gateway
 * both call this class so the mock catalog has one implementation.
 */
public final class OrderQueryService {

    private final OrderCatalog catalog;
    private final LocalDate today;

    public OrderQueryService(OrderCatalog catalog, LocalDate today) {
        this.catalog = catalog;
        this.today = today;
    }

    public OrderLookupResult lookup(String orderId, String customerPhone, String productKeyword, String timeHint) {
        String id = blankToNull(orderId);
        String phone = blankToNull(customerPhone);
        String keyword = blankToNull(productKeyword);
        if (id == null && phone == null) {
            return OrderLookupResult.badRequest("需要订单号或手机号");
        }
        List<Order> all = catalog.load(today);
        if (id != null) {
            List<Order> byId = all.stream()
                    .filter(order -> order.orderId().equalsIgnoreCase(id))
                    .filter(order -> phone == null || order.customerPhone().equals(phone))
                    .toList();
            if (byId.isEmpty()) {
                return OrderLookupResult.notFound("没有找到订单 " + id);
            }
            return OrderLookupResult.ok(byId);
        }
        List<Order> found = new ArrayList<>(all.stream().filter(order -> order.customerPhone().equals(phone)).toList());
        if (keyword != null) {
            String needle = keyword.toLowerCase(Locale.ROOT);
            found = found.stream()
                    .filter(order -> order.productName().toLowerCase(Locale.ROOT).contains(needle)
                            || order.category().label().contains(keyword))
                    .toList();
        }
        if (found.isEmpty()) {
            return OrderLookupResult.notFound("这个手机号下没有匹配的订单");
        }
        if ("last_week".equals(timeHint)) {
            List<Order> recent = found.stream().filter(order -> withinDays(order, 14)).toList();
            if (recent.size() == 1) {
                found = recent;
            } else if (recent.size() > 1) {
                found = recent;
            }
        }
        if (found.size() > 1) {
            return OrderLookupResult.ambiguous(found);
        }
        return OrderLookupResult.ok(found);
    }

    public LogisticsLookupResult logistics(String orderId) {
        String id = blankToNull(orderId);
        if (id == null) {
            return LogisticsLookupResult.badRequest("需要订单号");
        }
        return catalog.load(today).stream()
                .filter(order -> order.orderId().equalsIgnoreCase(id))
                .findFirst()
                .map(order -> LogisticsLookupResult.ok(toLogistics(order)))
                .orElseGet(() -> LogisticsLookupResult.notFound("没有找到订单 " + id + " 的物流"));
    }

    public Logistics toLogistics(Order order) {
        String tracking = "CG" + Math.abs(order.orderId().hashCode());
        List<LogisticsEvent> events = new ArrayList<>();
        LocalDate start = order.purchasedOn();
        events.add(new LogisticsEvent(start, "澄光速递已揽收"));
        events.add(new LogisticsEvent(start.plusDays(1), "运输中"));
        String statusText;
        LocalDate signed = order.signedOn();
        switch (order.status()) {
            case LOST -> {
                events.add(new LogisticsEvent(start.plusDays(3), "承运商确认包裹丢失"));
                statusText = "物流丢件";
                signed = null;
            }
            case SHIPPED -> {
                events.add(new LogisticsEvent(today, "运输中，尚未签收"));
                statusText = "运输中";
                signed = null;
            }
            default -> {
                LocalDate signDay = order.signedOn() == null ? today : order.signedOn();
                String signText = order.shippingDamage() ? "已签收，外包装破损" : "已签收";
                events.add(new LogisticsEvent(signDay, signText));
                statusText = order.status() == OrderStatus.REFUNDED ? "已签收，订单已退款" : "已签收";
            }
        }
        return new Logistics(order.orderId(), "澄光速递", tracking, statusText, signed, List.copyOf(events));
    }

    private boolean withinDays(Order order, int days) {
        LocalDate basis = order.signedOn() != null ? order.signedOn() : order.purchasedOn();
        return ChronoUnit.DAYS.between(basis, today) <= days;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
