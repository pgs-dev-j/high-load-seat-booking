package dev.booking.concurrency;

import org.springframework.test.context.ActiveProfiles;

/**
 * Stage 3 — expected to PASS. Same test, same base class, only the
 * profile changed. Correctness comes from @Version + retry in
 * OptimisticLockBookingService, not from any lock held during read.
 */
@ActiveProfiles("optimistic")
class OptimisticLockRaceConditionTest extends BookingRaceConditionTestBase {
}
