package dev.striveh.aftersales.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderToolsTest {

    @Autowired
    private OrderTools tools;

    @Test
    void lookupOrderReturnsJson() {
        String json = tools.lookupOrder(null, "13800001001", "耳机", "last_week");
        assertThat(json).contains("ORD-DEMO-1001").contains("林夏").contains("AirTune");
    }

    @Test
    void logisticsRequiresOrderId() {
        assertThat(tools.lookupLogistics(" ")).contains("BAD_REQUEST");
    }

    @Test
    void knownLogisticsIncludesCarrier() {
        assertThat(tools.lookupLogistics("ORD-DEMO-1004")).contains("澄光速递").contains("运输中");
    }
}
