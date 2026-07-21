package dev.booking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt = Instant.now();

    public Booking(Seat seat, String userId) {
        this.seat = seat;
        this.userId = userId;
    }
}
