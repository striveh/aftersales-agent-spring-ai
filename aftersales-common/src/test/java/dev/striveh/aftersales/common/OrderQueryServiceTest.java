package dev.striveh.aftersales.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderQueryServiceTest {

    private OrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new OrderQueryService(new OrderCatalog(), LocalDate.of(2026, 10, 19));
    }

    @Test
    void lookupRecentEarphoneByPhone() {
        OrderLookupResult result = service.lookup(null, "13800001001", "耳机", "last_week");
        assertTrue(result.foundOne());
        assertEquals("ORD-DEMO-1001", result.orders().get(0).orderId());
        assertEquals(LocalDate.of(2026, 10, 13), result.orders().get(0).signedOn());
    }

    @Test
    void unknownOrderIdIsNotFound() {
        OrderLookupResult result = service.lookup("ORD-00000000-0000", "13800001001", "耳机", null);
        assertEquals("NOT_FOUND", result.status());
    }

    @Test
    void twoEarphonesNeedClarificationUntilLastWeekIsApplied() {
        assertEquals("AMBIGUOUS", service.lookup(null, "13800001015", "耳机", null).status());
        OrderLookupResult narrowed = service.lookup(null, "13800001015", "耳机", "last_week");
        assertTrue(narrowed.foundOne());
        assertEquals("ORD-DEMO-1015A", narrowed.orders().get(0).orderId());
    }

    @Test
    void explicitIdWins() {
        OrderLookupResult result = service.lookup("ORD-DEMO-1015B", "13800001015", null, null);
        assertTrue(result.foundOne());
        assertEquals(40, java.time.temporal.ChronoUnit.DAYS.between(result.orders().get(0).signedOn(), LocalDate.of(2026, 10, 19)));
    }

    @Test
    void lostPackageHasNoSignoff() {
        LogisticsLookupResult logistics = service.logistics("ORD-DEMO-1005");
        assertEquals("OK", logistics.status());
        assertNull(logistics.logistics().signedOn());
        assertTrue(logistics.logistics().events().stream().anyMatch(event -> event.description().contains("丢失")));
    }

    @Test
    void missingIdentityIsRejected() {
        assertEquals("BAD_REQUEST", service.lookup(" ", " ", null, null).status());
    }
}
