package com.ceos22.cgv_clone.domain.reservation.entity;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_schedule_seat_position", columnNames = {"schedule_id", "seat_row", "seat_col"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private Integer seatRow;

    @Column(nullable = false)
    private Integer seatCol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeatStatus status;

    // Reservation that currently holds or owns the seat; null while AVAILABLE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    private LocalDateTime holdExpiresAt;

    @Builder
    public ScheduleSeat(Schedule schedule, Integer seatRow, Integer seatCol) {
        this.schedule = schedule;
        this.seatRow = seatRow;
        this.seatCol = seatCol;
        this.status = SeatStatus.AVAILABLE;
    }

    // An expired hold counts as available, so no cleanup job is needed before re-selling
    public boolean isAvailableAt(LocalDateTime now) {
        return status == SeatStatus.AVAILABLE
                || (status == SeatStatus.HELD && holdExpiresAt.isBefore(now));
    }

    /** @throws CustomException SEAT_ALREADY_RESERVED when another reservation holds or owns the seat */
    void hold(Reservation reservation, LocalDateTime expiresAt, LocalDateTime now) {
        if (!isAvailableAt(now)) {
            throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
        }
        this.status = SeatStatus.HELD;
        this.reservation = reservation;
        this.holdExpiresAt = expiresAt;
    }

    // The boundary matches isAvailableAt: a hold is still valid at exactly holdExpiresAt
    boolean isHeldBy(Reservation reservation, LocalDateTime now) {
        return status == SeatStatus.HELD && belongsTo(reservation) && !holdExpiresAt.isBefore(now);
    }

    /** @throws CustomException SEAT_HOLD_EXPIRED when the hold expired or moved to another reservation */
    void extendHold(Reservation reservation, LocalDateTime expiresAt, LocalDateTime now) {
        if (!isHeldBy(reservation, now)) {
            throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
        }
        this.holdExpiresAt = expiresAt;
    }

    void confirm(Reservation reservation) {
        if (status != SeatStatus.HELD || !belongsTo(reservation)) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
        this.status = SeatStatus.RESERVED;
        this.holdExpiresAt = null;
    }

    // No-op when an expired hold was already taken over by another reservation
    void release(Reservation reservation) {
        if (!belongsTo(reservation)) {
            return;
        }
        this.status = SeatStatus.AVAILABLE;
        this.reservation = null;
        this.holdExpiresAt = null;
    }

    private boolean belongsTo(Reservation reservation) {
        return this.reservation != null && this.reservation == reservation;
    }
}
