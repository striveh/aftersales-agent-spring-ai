package dev.striveh.aftersales.mcp;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.common.OrderCatalog;
import dev.striveh.aftersales.common.OrderQueryService;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * Read-only MCP tools. Refunds stay on the agent so a money movement cannot be
 * triggered by a tool call from this server.
 */
@Service
public class OrderTools {

    private final OrderQueryService orders;
    private final ObjectMapper json;

    public OrderTools(ObjectMapper json) {
        this.json = json;
        this.orders = new OrderQueryService(new OrderCatalog(), LocalDate.now(Clock.system(ZoneId.of("Asia/Shanghai"))));
    }

    @McpTool(name = "lookup_order", description = "按订单号或手机号查询澄光数码的模拟订单。可附加商品关键词和时间提示 last_week。")
    public String lookupOrder(
            @McpToolParam(description = "订单号，例如 ORD-DEMO-1001。未知时留空", required = false) String orderId,
            @McpToolParam(description = "下单手机号", required = false) String customerPhone,
            @McpToolParam(description = "商品关键词，例如耳机、坚果、会员", required = false) String productKeyword,
            @McpToolParam(description = "时间提示。上周或最近买的传 last_week，否则留空", required = false) String timeHint) {
        return write(orders.lookup(orderId, customerPhone, productKeyword, timeHint));
    }

    @McpTool(name = "lookup_logistics", description = "按订单号查询物流承运商、运单号、签收日和轨迹。")
    public String lookupLogistics(
            @McpToolParam(description = "订单号", required = true) String orderId) {
        return write(orders.logistics(orderId));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化工具结果", exception);
        }
    }
}
