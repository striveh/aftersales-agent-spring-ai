package dev.striveh.aftersales.agent.plan;

public record PlanContext(
        String message,
        String customerPhone,
        String knownOrderId,
        boolean previousRefundable,
        boolean ignoreOrderIdInMessage
) {

    public PlanContext withoutOrderId() {
        return new PlanContext(message, customerPhone, knownOrderId, previousRefundable, true);
    }

    public String message() {
        return message == null ? "" : message;
    }

    public String customerPhone() {
        return customerPhone == null ? "" : customerPhone;
    }

    public String knownOrderId() {
        return knownOrderId == null ? "" : knownOrderId;
    }
}
