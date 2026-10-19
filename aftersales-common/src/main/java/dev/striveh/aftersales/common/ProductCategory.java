package dev.striveh.aftersales.common;

public enum ProductCategory {
    ELECTRONICS("数码配件"),
    CUSTOM("定制"),
    FOOD("食品"),
    INTIMATE("贴身衣物"),
    DIGITAL("数字商品");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
