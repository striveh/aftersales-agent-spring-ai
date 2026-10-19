package dev.striveh.aftersales.agent.tool;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import dev.striveh.aftersales.common.Order;

public final class ApprovalService {

    private final Clock clock;
    private final RefundLedger ledger;
    private final ConcurrentMap<String, Approval> approvals = new ConcurrentHashMap<>();

    public ApprovalService(Clock clock, RefundLedger ledger) {
        this.clock = clock;
        this.ledger = ledger;
    }

    public Approval open(String conversationId, Order order, String reason) {
        Approval approval = new Approval(
                "APR-" + UUID.randomUUID().toString().substring(0, 8),
                conversationId,
                order.orderId(),
                order.customerName(),
                order.amount(),
                reason,
                ApprovalStatus.PENDING,
                clock.instant());
        approvals.put(approval.id(), approval);
        return approval;
    }

    public Optional<Approval> find(String id) {
        return Optional.ofNullable(approvals.get(id));
    }

    public Approval decide(String id, boolean approved) {
        Approval current = approvals.get(id);
        if (current == null) {
            throw new ApprovalNotFoundException(id);
        }
        if (current.status() != ApprovalStatus.PENDING) {
            return current;
        }
        Approval updated = new Approval(current.id(), current.conversationId(), current.orderId(), current.customerName(),
                current.amount(), current.reason(), approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED,
                current.createdAt());
        approvals.put(id, updated);
        if (approved) {
            ledger.record(updated);
        }
        return updated;
    }

    public static final class ApprovalNotFoundException extends RuntimeException {
        public ApprovalNotFoundException(String id) {
            super("没有这笔确认单 " + id);
        }
    }
}
