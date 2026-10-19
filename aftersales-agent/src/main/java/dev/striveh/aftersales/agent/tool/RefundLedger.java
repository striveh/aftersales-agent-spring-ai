package dev.striveh.aftersales.agent.tool;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Demo ledger. Recording a refund does not move real money. */
public final class RefundLedger {

    private final Clock clock;
    private final ConcurrentMap<String, Refund> byOrderId = new ConcurrentHashMap<>();

    public RefundLedger(Clock clock) {
        this.clock = clock;
    }

    public Refund record(Approval approval) {
        if (approval.status() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("未确认的申请不能入账");
        }
        Refund refund = new Refund(approval.id(), approval.orderId(), approval.amount(), clock.instant());
        byOrderId.putIfAbsent(approval.orderId(), refund);
        return byOrderId.get(approval.orderId());
    }

    public boolean exists(String orderId) {
        return byOrderId.containsKey(orderId);
    }

    public Optional<Refund> find(String orderId) {
        return Optional.ofNullable(byOrderId.get(orderId));
    }

    public int size() {
        return byOrderId.size();
    }
}
