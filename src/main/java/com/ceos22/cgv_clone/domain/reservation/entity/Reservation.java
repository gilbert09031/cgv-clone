package com.ceos22.cgv_clone.domain.reservation.entity;

import com.ceos22.cgv_clone.common.entity.BaseEntity;
import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    // How long seats stay held while the member completes payment.
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private Integer totalPrice;

    // 좌석별로 취소 불가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    @Builder
    public Reservation(Member member, Schedule schedule, Integer totalPrice) {
        this.member = member;
        this.schedule = schedule;
        this.totalPrice = totalPrice;
        this.status = ReservationStatus.PENDING;
    }

    /** @throws CustomException SEAT_ALREADY_RESERVED when any seat is held or owned by another reservation */
    public void holdSeats(List<ScheduleSeat> seats, LocalDateTime now) {
        validateStatus(ReservationStatus.PENDING);
        LocalDateTime expiresAt = now.plus(HOLD_DURATION);
        for (ScheduleSeat seat : seats) {
            if (!seat.getSchedule().getScheduleId().equals(schedule.getScheduleId())) {
                throw new CustomException(ErrorCode.SEAT_NOT_FOUND);
            }
            seat.hold(this, expiresAt, now);
            reservationSeats.add(ReservationSeat.builder()
                    .reservation(this)
                    .scheduleSeat(seat)
                    .build());
        }
    }

    /**
     * Restart the hold from now, used right before the gateway approval so the hold outlives the gateway call
     * @throws CustomException SEAT_HOLD_EXPIRED when any seat is no longer held by this reservation
     */
    public void extendHold(LocalDateTime now) {
        validateStatus(ReservationStatus.PENDING);
        LocalDateTime expiresAt = now.plus(HOLD_DURATION);
        reservationSeats.forEach(reservationSeat -> reservationSeat.getScheduleSeat().extendHold(this, expiresAt, now));
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getMemberId().equals(memberId);
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    // Every seat still held by this reservation. Checks only; nothing is locked or changed
    public boolean hasValidHoldAt(LocalDateTime now) {
        return !reservationSeats.isEmpty()
                && reservationSeats.stream().allMatch(reservationSeat -> reservationSeat.getScheduleSeat().isHeldBy(this, now));
    }

    // Title shown in the payment widget, e.g. "영화명 2매"
    public String summaryName() {
        return String.format("%s %d매", schedule.getMovie().getTitle(), reservationSeats.size());
    }

    public void confirm() {
        validateStatus(ReservationStatus.PENDING);
        reservationSeats.forEach(reservationSeat -> reservationSeat.getScheduleSeat().confirm(this));
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
        releaseSeats();
        this.status = ReservationStatus.CANCELED;
    }

    public void expire() {
        validateStatus(ReservationStatus.PENDING);
        releaseSeats();
        this.status = ReservationStatus.EXPIRED;
    }

    private void releaseSeats() {
        reservationSeats.forEach(reservationSeat -> reservationSeat.getScheduleSeat().release(this));
    }

    private void validateStatus(ReservationStatus expected) {
        if (status != expected) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
    }
}
