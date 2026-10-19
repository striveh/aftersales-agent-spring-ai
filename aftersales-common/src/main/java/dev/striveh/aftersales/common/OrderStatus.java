package dev.striveh.aftersales.common;

public enum OrderStatus {
    SHIPPED("运输中"),
    SIGNED("已签收"),
    LOST("物流丢件"),
    REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
