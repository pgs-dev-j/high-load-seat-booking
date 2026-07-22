package dev.booking.concurrency;

import org.springframework.test.context.ActiveProfiles;

/**
 * Stage 2 — expected to PASS. Same test as Stage 1, only the profile
 * changed. SELECT ... FOR UPDATE in PessimisticLockBookingService closes
 * the race window that NaiveBookingService left open.
 */
@ActiveProfiles("pessimistic")
class PessimisticLockRaceConditionTest extends BookingRaceConditionTestBase {
}
