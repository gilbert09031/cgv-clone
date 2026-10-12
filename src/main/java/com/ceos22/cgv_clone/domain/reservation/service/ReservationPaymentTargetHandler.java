package com.ceos22.cgv_clone.domain.reservation.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import com.ceos22.cgv_clone.domain.payment.target.PayableTarget;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import com.ceos22.cgv_clone.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Implementation of PaymentTargetHandler for movie reservations.
 * Runs inside the payment service's transactions; state changes are delegated to ReservationService
 */
@Component
@RequiredArgsConstructor
public class ReservationPaymentTargetHandler implements PaymentTargetHandler {

    private final ReservationService reservationService;

    @Override
    public PaymentTargetType supports() {
        return PaymentTargetType.RESERVATION;
    }

    @Override
    public PayableTarget verifyPayable(Long targetId, Long memberId) {
        Reservation reservation = reservationService.findReservationWithSeats(targetId);

        if (!reservation.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_FAILED);
        }
        if (!reservation.isPending()) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
        if (!reservation.hasValidHoldAt(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.SEAT_HOLD_EXPIRED);
        }
        return new PayableTarget(reservation.getTotalPrice(), reservation.summaryName());
    }

    // RESERVATION policy: seats are held when the reservation is created; here the hold is locked and restarted
    @Override
    public void onApprovalStarted(Long targetId, String paymentNumber, String gatewayOrderId) {
        reservationService.preemptSeatsForPayment(targetId, paymentNumber, gatewayOrderId);
    }

    @Override
    public void onApproved(Long targetId, String paymentNumber, String gatewayOrderId) {
        reservationService.confirmReservationAfterPayment(targetId, paymentNumber, gatewayOrderId);
    }

    @Override
    public void onApprovalFailed(Long targetId) {
    }

    @Override
    public void onCancelled(Long targetId) {
        reservationService.cancelReservationAfterPaymentCancel(targetId);
    }
}
