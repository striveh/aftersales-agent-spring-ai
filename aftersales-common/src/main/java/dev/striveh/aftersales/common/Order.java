package dev.striveh.aftersales.common;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Order(
        String orderId,
        String customerPhone,
        String customerName,
        String productName,
        ProductCategory category,
        BigDecimal amount,
        LocalDate purchasedOn,
        LocalDate signedOn,
        OrderStatus status,
        boolean accessoriesComplete,
        boolean humanDamage,
        boolean shippingDamage,
        boolean activated,
        String note
) {
}
