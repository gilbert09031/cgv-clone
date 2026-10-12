package com.ceos22.cgv_clone.domain.reservation.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.logging.TransactionLog;
import com.ceos22.cgv_clone.domain.member.entity.Member;
import com.ceos22.cgv_clone.domain.member.repository.MemberRepository;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import com.ceos22.cgv_clone.domain.payment.service.PaymentProgressReader;
import com.ceos22.cgv_clone.domain.reservation.dto.request.ReservationCreateRequest;
import com.ceos22.cgv_clone.domain.reservation.dto.response.ReservationResponse;
import com.ceos22.cgv_clone.domain.reservation.entity.Reservation;
import com.ceos22.cgv_clone.domain.reservation.entity.ReservationStatus;
import com.ceos22.cgv_clone.domain.reservation.entity.Schedule;
import com.ceos22.cgv_clone.domain.reservation.entity.ScheduleSeat;
import com.ceos22.cgv_clone.domain.reservation.repository.ReservationRepository;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleRepository;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    // TODO: replace with ScreenType x AudienceType pricing once price rules are decided
    private static final int TICKET_PRICE = 10_000;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository; // MemberRepository 주입
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final PaymentProgressReader paymentProgressReader;

    public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
        Member member = findMemberById(memberId);
        Schedule schedule = findScheduleById(request.scheduleId());

        List<Reservation> replacedReservations = releasePendingReservations(member, schedule);

        List<Long> scheduleSeatIds = request.scheduleSeatIds().stream().distinct().toList();
        List<ScheduleSeat> seats = scheduleSeatRepository.findAllForUpdate(schedule.getScheduleId(), scheduleSeatIds);
        if(seats.size() != scheduleSeatIds.size()) {
            throw new CustomException(ErrorCode.SEAT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.builder()
                .member(member)
                .schedule(schedule)
                .totalPrice(totalPriceOf(seats))
                .build();
        reservation.holdSeats(seats, now);
        reservationRepository.save(reservation);

        logReservationCreated(reservation);
        replacedReservations.forEach(replaced -> logReservationReplaced(replaced, reservation));

        return ReservationResponse.from(reservation);
    }

    /**
     * Take the seats out of reach of other members for the whole approval, run before the gateway is called.
     * Locks the seat rows and restarts the hold, so an expired hold fails here instead of after the money moved
     * @throws CustomException SEAT_HOLD_EXPIRED when a seat is no longer held by this reservation
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void preemptSeatsForPayment(Long reservationId, String paymentNumber, String gatewayOrderId) {
        Reservation reservation = findReservationWithSeats(reservationId);

        List<ScheduleSeat> lockedSeats = scheduleSeatRepository.findAllForUpdateByReservationId(reservationId);
        if (lockedSeats.size() != reservation.getReservationSeats().size()) {
            throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
        }
        reservation.extendHold(LocalDateTime.now());

        logReservationSeatsPreempted(reservation, paymentNumber, gatewayOrderId);
    }

    // Payment is confirmed, so this only transits the reservation state
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmReservationAfterPayment(Long reservationId, String paymentNumber, String gatewayOrderId) {
        Reservation reservation = findReservationWithSeats(reservationId);
        reservation.confirm();

        logReservationConfirmed(reservationId, paymentNumber, gatewayOrderId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelReservationAfterPaymentCancel(Long reservationId) {
        Reservation reservation = findReservationWithSeats(reservationId);
        reservation.cancel();
    }

    @Transactional(readOnly = true)
    public Reservation findReservationWithSeats(Long reservationId) {
        return reservationRepository.findWithSeatsById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findMyReservations(Long memberId) {
        Member member = findMemberById(memberId);
        List<Reservation> reservations = findReservationByMember(member);

        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    //==================================================================================================================

    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Schedule findScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private List<Reservation> releasePendingReservations(Member member, Schedule schedule) {
        List<Reservation> pendingReservations = reservationRepository
                .findAllWithSeatsByMemberAndScheduleAndStatus(member, schedule, ReservationStatus.PENDING);

        for (Reservation reservation : pendingReservations) {
            if (paymentProgressReader.hasApprovalInProgress(PaymentTargetType.RESERVATION, reservation.getReservationId())) {
                throw new CustomException(ErrorCode.RESERVATION_PAYMENT_IN_PROGRESS);
            }
            reservation.cancel();
        }
        return pendingReservations;
    }

    private int totalPriceOf(List<ScheduleSeat> seats) {
        return TICKET_PRICE * seats.size();
    }

    public List<Reservation> findReservationByMember(Member member) {
        return reservationRepository.findByMember(member)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void logReservationCreated(Reservation reservation) {
        Long reservationId = reservation.getReservationId();
        Long scheduleId = reservation.getSchedule().getScheduleId();
        int seatCount = reservation.getReservationSeats().size();
        Integer amount = reservation.getTotalPrice();
        String holdExpiresAt = holdExpiresAtOf(reservation);
        TransactionLog.afterCommit(() -> {
            LoggingEventBuilder event = log.atInfo()
                    .addKeyValue("event", "reservation.created")
                    .addKeyValue("reservationId", reservationId)
                    .addKeyValue("scheduleId", scheduleId)
                    .addKeyValue("seatCount", seatCount)
                    .addKeyValue("amount", amount);
            addHoldExpiresAt(event, holdExpiresAt).log();
        });
    }

    private void logReservationReplaced(Reservation reservation, Reservation replacement) {
        Long reservationId = reservation.getReservationId();
        Long replacementReservationId = replacement.getReservationId();
        TransactionLog.afterCommit(() -> log.atInfo()
                .addKeyValue("event", "reservation.cancelled")
                .addKeyValue("reservationId", reservationId)
                .addKeyValue("replacementReservationId", replacementReservationId)
                .addKeyValue("reason", "reservation_replaced")
                .log());
    }

    private void logReservationSeatsPreempted(Reservation reservation, String paymentNumber, String gatewayOrderId) {
        Long reservationId = reservation.getReservationId();
        Long scheduleId = reservation.getSchedule().getScheduleId();
        int seatCount = reservation.getReservationSeats().size();
        String holdExpiresAt = holdExpiresAtOf(reservation);
        TransactionLog.afterCommit(() -> {
            LoggingEventBuilder event = log.atInfo()
                    .addKeyValue("event", "reservation.seats_preempted")
                    .addKeyValue("reservationId", reservationId)
                    .addKeyValue("scheduleId", scheduleId)
                    .addKeyValue("seatCount", seatCount)
                    .addKeyValue("paymentNumber", paymentNumber)
                    .addKeyValue("gatewayOrderId", gatewayOrderId);
            addHoldExpiresAt(event, holdExpiresAt).log();
        });
    }

    private void logReservationConfirmed(Long reservationId, String paymentNumber, String gatewayOrderId) {
        TransactionLog.afterCommit(() -> log.atInfo()
                .addKeyValue("event", "reservation.confirmed")
                .addKeyValue("reservationId", reservationId)
                .addKeyValue("paymentNumber", paymentNumber)
                .addKeyValue("gatewayOrderId", gatewayOrderId)
                .log());
    }

    private String holdExpiresAtOf(Reservation reservation) {
        return reservation.getReservationSeats().stream()
                .map(reservationSeat -> reservationSeat.getScheduleSeat().getHoldExpiresAt())
                .findFirst()
                .map(holdExpiresAt -> holdExpiresAt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
                .orElse(null);
    }

    private LoggingEventBuilder addHoldExpiresAt(LoggingEventBuilder event, String holdExpiresAt) {
        if (holdExpiresAt == null) {
            return event;
        }
        return event.addKeyValue("holdExpiresAt", holdExpiresAt);
    }
}
