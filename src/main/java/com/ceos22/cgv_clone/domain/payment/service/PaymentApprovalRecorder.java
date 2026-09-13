package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.gateway.GatewayApproval;
import com.ceos22.cgv_clone.domain.payment.repository.PaymentRepository;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandlers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentApprovalRecorder {

    private final PaymentRepository paymentRepository;
    private final PaymentTargetHandlers targetHandlers;
    private final long maxFailureCount;

    public PaymentApprovalRecorder(PaymentRepository paymentRepository,
                                   PaymentTargetHandlers targetHandlers,
                                   @Value("${payment.max-failure-count}") long maxFailureCount) {
        this.paymentRepository = paymentRepository;
        this.targetHandlers = targetHandlers;
        this.maxFailureCount = maxFailureCount;
    }

    /**
     * Claim the payment for one approval attempt and reserve what the target needs to be fulfilled
     * @return payment method, which selects the gateway to call next
     * @throws CustomException when the payment or its target cannot start an approval; nothing is reserved then
     */
    @Transactional
    public PaymentMethod preempt(Long memberId, String paymentNumber, Integer amount) {
        List<Payment> paymentsOfTarget = lockPaymentsOfSameTarget(paymentNumber);
        Payment payment = selectByNumber(paymentsOfTarget, paymentNumber);

        validateOwner(payment, memberId);
        if (!payment.isOpen()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        if (!payment.hasAmount(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (payment.failureCount() >= maxFailureCount) {
            throw new CustomException(ErrorCode.PAYMENT_RETRY_EXCEEDED);
        }
        if (paymentsOfTarget.stream().anyMatch(Payment::isInProgressOrDone)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        PaymentTarget target = payment.getTarget();
        PaymentTargetHandler handler = targetHandlers.get(target.getType());
        handler.verifyPayable(target.getId(), memberId);
        handler.onApprovalStarted(target.getId());

        payment.startApproval(LocalDateTime.now());
        return payment.getMethod();
    }

    /** Record the approval and confirm the target. The money has moved, so this must not fail for business reasons */
    @Transactional
    public PaymentResponse recordSuccess(String paymentNumber, GatewayApproval approval) {
        Payment payment = selectByNumber(lockPaymentsOfSameTarget(paymentNumber), paymentNumber);

        payment.recordApproval(approval.transactionId(), approval.approvedAt());
        handlerFor(payment).onApproved(payment.getTarget().getId());

        return PaymentResponse.from(payment);
    }

    /** Record the gateway's rejection and release what preempt reserved */
    @Transactional
    public void recordFailure(String paymentNumber, String failureCode, String failureReason) {
        Payment payment = selectByNumber(lockPaymentsOfSameTarget(paymentNumber), paymentNumber);

        payment.recordFailure(failureCode, failureReason, LocalDateTime.now());
        handlerFor(payment).onApprovalFailed(payment.getTarget().getId());
    }

    private List<Payment> lockPaymentsOfSameTarget(String paymentNumber) {
        PaymentTarget target = paymentRepository.findTargetByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return paymentRepository.lockAllByTarget(target);
    }

    private Payment selectByNumber(List<Payment> paymentsOfTarget, String paymentNumber) {
        return paymentsOfTarget.stream()
                .filter(payment -> payment.hasNumber(paymentNumber))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentTargetHandler handlerFor(Payment payment) {
        return targetHandlers.get(payment.getTarget().getType());
    }

    private void validateOwner(Payment payment, Long memberId) {
        if (!payment.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_FAILED);
        }
    }
}
