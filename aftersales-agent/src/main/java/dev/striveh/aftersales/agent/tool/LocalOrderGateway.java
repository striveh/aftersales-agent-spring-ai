package dev.striveh.aftersales.agent.tool;

import java.time.LocalDate;

import dev.striveh.aftersales.common.LogisticsLookupResult;
import dev.striveh.aftersales.common.OrderCatalog;
import dev.striveh.aftersales.common.OrderLookupResult;
import dev.striveh.aftersales.common.OrderQueryService;

/** Same catalog the MCP server exposes, called in-process for tests and the default profile. */
public final class LocalOrderGateway implements OrderGateway {

    private final OrderQueryService orders;

    public LocalOrderGateway(LocalDate today) {
        this.orders = new OrderQueryService(new OrderCatalog(), today);
    }

    @Override
    public OrderLookupResult lookup(String orderId, String customerPhone, String productKeyword, String timeHint) {
        return orders.lookup(orderId, customerPhone, productKeyword, timeHint);
    }

    @Override
    public LogisticsLookupResult logistics(String orderId) {
        return orders.logistics(orderId);
    }
}
