package com.ceos22.cgv_clone.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Seat included in a reservation. Kept after cancellation as history; the live state lives in ScheduleSeat */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_seat", columnNames = {"reservation_id", "schedule_seat_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    private ScheduleSeat scheduleSeat;

    // Only Reservation creates seats, so the hold and the history row stay in step
    @Builder(access = AccessLevel.PACKAGE)
    ReservationSeat(Reservation reservation, ScheduleSeat scheduleSeat) {
        this.reservation = reservation;
        this.scheduleSeat = scheduleSeat;
    }
}
