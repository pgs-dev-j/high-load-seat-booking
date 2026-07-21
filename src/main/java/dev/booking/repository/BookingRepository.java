package dev.booking.repository;

import dev.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findBySeatId(Long seatId);

    long countBySeatId(Long seatId);
}
