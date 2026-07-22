package dev.booking.concurrency;

import org.springframework.test.context.ActiveProfiles;

/**
 * Stage 1 — expected to FAIL. That failure is the automated proof of the
 * double-booking bug in NaiveBookingService. See BookingRaceConditionTestBase
 * for the actual test logic.
 */
@ActiveProfiles("naive")
class NaiveBookingRaceConditionTest extends BookingRaceConditionTestBase {
}
