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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

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
    public void holdSeats(List<ScheduleSeat> seats, LocalDateTime expiresAt, LocalDateTime now) {
        validateStatus(ReservationStatus.PENDING);
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
