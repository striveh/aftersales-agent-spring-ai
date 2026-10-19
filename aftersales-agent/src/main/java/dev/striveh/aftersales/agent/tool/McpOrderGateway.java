package dev.striveh.aftersales.agent.tool;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.common.LogisticsLookupResult;
import dev.striveh.aftersales.common.OrderLookupResult;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

public final class McpOrderGateway implements OrderGateway {

    private final ToolCallbackProvider tools;
    private final ObjectMapper json;

    public McpOrderGateway(ToolCallbackProvider tools, ObjectMapper json) {
        this.tools = tools;
        this.json = json;
    }

    @Override
    public OrderLookupResult lookup(String orderId, String customerPhone, String productKeyword, String timeHint) {
        Map<String, String> args = new LinkedHashMap<>();
        args.put("orderId", empty(orderId));
        args.put("customerPhone", empty(customerPhone));
        args.put("productKeyword", empty(productKeyword));
        args.put("timeHint", empty(timeHint));
        return read(call("lookup_order", args), OrderLookupResult.class);
    }

    @Override
    public LogisticsLookupResult logistics(String orderId) {
        return read(call("lookup_logistics", Map.of("orderId", empty(orderId))), LogisticsLookupResult.class);
    }

    private String call(String name, Map<String, String> args) {
        ToolCallback callback = find(name);
        try {
            return callback.call(json.writeValueAsString(args));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法构造 MCP 参数", exception);
        }
    }

    private ToolCallback find(String name) {
        return Arrays.stream(tools.getToolCallbacks())
                .filter(callback -> matches(callback.getToolDefinition().name(), name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP 工具不存在: " + name));
    }

    static boolean matches(String actual, String expected) {
        return actual.equals(expected) || actual.endsWith("_" + expected) || actual.endsWith("/" + expected);
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return json.readValue(body, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析 MCP 工具结果: " + body, exception);
        }
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
