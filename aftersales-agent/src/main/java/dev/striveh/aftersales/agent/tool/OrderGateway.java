package dev.striveh.aftersales.agent.tool;

import dev.striveh.aftersales.common.LogisticsLookupResult;
import dev.striveh.aftersales.common.OrderLookupResult;

public interface OrderGateway {

    OrderLookupResult lookup(String orderId, String customerPhone, String productKeyword, String timeHint);

    LogisticsLookupResult logistics(String orderId);
}
