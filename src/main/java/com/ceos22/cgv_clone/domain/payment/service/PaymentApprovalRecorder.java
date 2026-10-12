package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.logging.TransactionLog;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.gateway.GatewayApproval;
import com.ceos22.cgv_clone.domain.payment.repository.PaymentRepository;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
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
     * Claim the attempt for one approval and reserve what the target needs to be fulfilled
     * @param gatewayOrderId order id the client received at preparation and used in the payment widget
     * @return payment number and method, which select and connect the gateway call
     * @throws CustomException when the payment or its target cannot start an approval; nothing is reserved then
     */
    @Transactional
    public ApprovalAttempt preempt(Long memberId, String gatewayOrderId, Integer amount) {
        Payment payment = lockPaymentOfAttempt(gatewayOrderId);

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

        PaymentTarget target = payment.getTarget();
        PaymentTargetHandler handler = targetHandlers.get(target.getType());
        handler.verifyPayable(target.getId(), memberId);
        handler.onApprovalStarted(target.getId(), payment.getPaymentNumber(), gatewayOrderId);

        payment.startApproval(gatewayOrderId, LocalDateTime.now());
        String paymentNumber = payment.getPaymentNumber();
        PaymentMethod method = payment.getMethod();

        TransactionLog.afterCommit(() -> log.atInfo()
                .addKeyValue("event", "payment.approval_started")
                .addKeyValue("paymentNumber", paymentNumber)
                .addKeyValue("gatewayOrderId", gatewayOrderId)
                .log());

        return new ApprovalAttempt(paymentNumber, method);
    }

    /** Record the approval and confirm the target. The money has moved, so this must not fail for business reasons */
    @Transactional
    public PaymentResponse recordSuccess(String gatewayOrderId, GatewayApproval approval) {
        Payment payment = lockPaymentOfAttempt(gatewayOrderId);

        payment.recordApproval(approval.transactionId(), approval.approvedAt());
        handlerFor(payment).onApproved(payment.getTarget().getId(), payment.getPaymentNumber(), gatewayOrderId);
        String paymentNumber = payment.getPaymentNumber();
        Integer amount = payment.getAmount();

        TransactionLog.afterCommit(() -> log.atInfo()
                .addKeyValue("event", "payment.approved")
                .addKeyValue("paymentNumber", paymentNumber)
                .addKeyValue("gatewayOrderId", gatewayOrderId)
                .addKeyValue("amount", amount)
                .log());

        return PaymentResponse.from(payment);
    }

    /** Record the gateway's rejection and release what preempt reserved */
    @Transactional
    public void recordFailure(String gatewayOrderId, String failureCode, String failureReason) {
        Payment payment = lockPaymentOfAttempt(gatewayOrderId);

        payment.recordFailure(failureCode, failureReason, LocalDateTime.now());
        handlerFor(payment).onApprovalFailed(payment.getTarget().getId());
        String paymentNumber = payment.getPaymentNumber();

        TransactionLog.afterCommit(() -> {
            var event = log.atWarn()
                    .addKeyValue("event", "payment.approval_rejected")
                    .addKeyValue("paymentNumber", paymentNumber)
                    .addKeyValue("gatewayOrderId", gatewayOrderId);
            if (failureCode != null && !failureCode.isBlank()) {
                event.addKeyValue("failureCode", failureCode);
            }
            event.log();
        });
    }

    private Payment lockPaymentOfAttempt(String gatewayOrderId) {
        PaymentTarget target = paymentRepository.findTargetByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        List<Payment> paymentsOfTarget = paymentRepository.lockAllByTarget(target);

        return paymentsOfTarget.stream()
                .filter(payment -> payment.hasAttempt(gatewayOrderId))
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

    public record ApprovalAttempt(String paymentNumber, PaymentMethod method) {}
}
