package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.metrics.PaymentMetrics;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentPrepareResponse;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.gateway.GatewayApproval;
import com.ceos22.cgv_clone.domain.payment.gateway.PaymentGateway;
import com.ceos22.cgv_clone.domain.payment.gateway.PaymentGatewayException;
import com.ceos22.cgv_clone.domain.payment.repository.PaymentRepository;
import com.ceos22.cgv_clone.domain.payment.target.PayableTarget;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    // Stock is taken with optimistic locking, so a preemption can lose a race that a fresh attempt wins
    private static final int MAX_PREEMPT_ATTEMPTS = 3;

    private final PaymentRepository paymentRepository;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final PaymentTargetHandlers targetHandlers;
    private final PaymentApprovalRecorder approvalRecorder;
    private final PaymentMetrics paymentMetrics;
    private final Map<PaymentMethod, PaymentGateway> gateways;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentNumberGenerator paymentNumberGenerator,
                          PaymentTargetHandlers targetHandlers,
                          PaymentApprovalRecorder approvalRecorder,
                          PaymentMetrics paymentMetrics,
                          List<PaymentGateway> gateways) {
        this.paymentRepository = paymentRepository;
        this.paymentNumberGenerator = paymentNumberGenerator;
        this.targetHandlers = targetHandlers;
        this.approvalRecorder = approvalRecorder;
        this.paymentMetrics = paymentMetrics;
        this.gateways = gateways.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentGateway::method, Function.identity()));
        validateEveryMethodHasGateway();
    }

    /**
     * 1. Verify that the target is payable and has no APPROVING or APPROVED payment
     * 2. Reuse the open payment of the target, or create one
     * 3. Issue an order id for this attempt; a new widget session always needs a new one
     */
    @Transactional
    public PaymentPrepareResponse prepare(Long memberId, PaymentTarget target, PaymentMethod method) {
        PaymentTargetHandler handler = targetHandlers.get(target.getType());
        PayableTarget payable = handler.verifyPayable(target.getId(), memberId);

        Payment payment = openPaymentOf(target, memberId, payable.amount(), method);
        String gatewayOrderId = payment.startAttempt(LocalDateTime.now());

        log.info("event=payment.attempt_prepared, paymentNumber={}, orderId={}, targetType={}, targetId={}, amount={}",
                payment.getPaymentNumber(), gatewayOrderId, target.getType(), target.getId(), payment.getAmount());

        return PaymentPrepareResponse.of(payment, gatewayOrderId, payable.title());
    }

    /**
     * Approve one attempt: preemption, gateway call, recording. Runs without a transaction so the gateway call
     * holds no connection and no locks
     * @param gatewayOrderId order id the client received at preparation
     * @param gatewayToken opaque value the client received from the payment widget; never parsed or stored
     * @throws PaymentGatewayException when the gateway rejects; the payment is FAILED and a new attempt may follow
     * @throws CustomException with PAYMENT_RESULT_UNKNOWN when the outcome is undecided; the payment stays APPROVING
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse confirm(Long memberId, String gatewayOrderId, String gatewayToken, Integer amount) {
        PaymentMethod method = preempt(memberId, gatewayOrderId, amount);
        log.info("event=payment.approval_started, orderId={}, amount={}", gatewayOrderId, amount);

        GatewayApproval approval = approve(method, gatewayOrderId, gatewayToken, amount);

        PaymentResponse response = recordSuccess(gatewayOrderId, approval);

        paymentMetrics.incrementPaymentSuccess();
        log.info("event=payment.approved, orderId={}, transactionId={}, amount={}",
                gatewayOrderId, approval.transactionId(), amount);

        return response;
    }

    public PaymentResponse getPayment(Long memberId, String paymentNumber) {
        Payment payment = findByPaymentNumber(paymentNumber);
        validateOwner(payment, memberId);
        return PaymentResponse.from(payment);
    }

    //=================================================================================================================

    // Locked here so that two preparations of the same target cannot create two payments
    private Payment openPaymentOf(PaymentTarget target, Long memberId, Integer amount, PaymentMethod method) {
        List<Payment> paymentsOfTarget = paymentRepository.lockAllByTarget(target);

        if (paymentsOfTarget.stream().anyMatch(Payment::isInProgressOrDone)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        return paymentsOfTarget.stream()
                .filter(payment -> payment.isOpen() && payment.isOwnedBy(memberId)
                        && payment.hasAmount(amount) && payment.hasMethod(method))
                .findFirst()
                .orElseGet(() -> paymentRepository.save(Payment.createPending(
                        paymentNumberGenerator.generate(target.getType()),
                        memberId,
                        target,
                        amount,
                        method,
                        LocalDateTime.now()
                )));
    }

    private PaymentMethod preempt(Long memberId, String gatewayOrderId, Integer amount) {
        for (int attempt = 1; ; attempt++) {
            try {
                return approvalRecorder.preempt(memberId, gatewayOrderId, amount);
            } catch (OptimisticLockingFailureException e) {
                boolean lastAttempt = attempt == MAX_PREEMPT_ATTEMPTS;
                log.warn("event=payment.preempt_conflicted, orderId={}, attempt={}/{}, retrying={}",
                        gatewayOrderId, attempt, MAX_PREEMPT_ATTEMPTS, !lastAttempt);
                if (lastAttempt) {
                    throw new CustomException(ErrorCode.PAYMENT_FAILED, "stock contention on " + gatewayOrderId);
                }
            }
        }
    }

    private GatewayApproval approve(PaymentMethod method, String gatewayOrderId, String gatewayToken, Integer amount) {
        try {
            return gateways.get(method).approve(gatewayOrderId, gatewayToken, amount);
        } catch (PaymentGatewayException e) {
            // Recorded before rethrowing: throwing inside the recorder would roll the failure entry back
            approvalRecorder.recordFailure(gatewayOrderId, e.getFailureCode(), e.getMessage());
            paymentMetrics.incrementPaymentFailure();
            log.warn("event=payment.approval_rejected, orderId={}, failureCode={}",
                    gatewayOrderId, e.getFailureCode());
            throw e;
        } catch (RuntimeException e) {
            // Nothing is recorded: the payment stays APPROVING and the sweep settles it against the gateway
            log.error("event=payment.approval_unknown, orderId={}", gatewayOrderId, e);
            throw new CustomException(ErrorCode.PAYMENT_RESULT_UNKNOWN);
        }
    }

    private PaymentResponse recordSuccess(String gatewayOrderId, GatewayApproval approval) {
        try {
            return approvalRecorder.recordSuccess(gatewayOrderId, approval);
        } catch (RuntimeException e) {
            // The gateway approved but the ledger does not know it yet; the transaction id is the key to reconcile
            log.error("event=payment.approval_record_failed, orderId={}, transactionId={}",
                    gatewayOrderId, approval.transactionId(), e);
            throw new CustomException(ErrorCode.PAYMENT_RESULT_UNKNOWN);
        }
    }

    private void validateEveryMethodHasGateway() {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (!gateways.containsKey(method)) {
                throw new IllegalStateException("No PaymentGateway for " + method);
            }
        }
    }

    private Payment findByPaymentNumber(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateOwner(Payment payment, Long memberId) {
        if (!payment.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_FAILED);
        }
    }
}
