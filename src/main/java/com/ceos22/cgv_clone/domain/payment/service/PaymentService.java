package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.metrics.PaymentMetrics;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentPrepareResponse;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
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

    // PENDING and FAILED do not block: failures inside the widget never reach the server and stay PENDING
    private static final List<PaymentStatus> IN_PROGRESS_OR_DONE =
            List.of(PaymentStatus.APPROVING, PaymentStatus.APPROVED);

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
     * 1. Verify that the target is Payable & there is NO Payment Object that has the same Target
     * 2. Generate a new PENDING Payment Object
     */
    @Transactional
    public PaymentPrepareResponse prepare(Long memberId, PaymentTarget target, PaymentMethod method) {
        PaymentTargetHandler handler = targetHandlers.get(target.getType());
        PayableTarget payable = handler.verifyPayable(target.getId(), memberId);
        validateNoPaymentInProgress(target);

        Payment payment = Payment.createPending(
                paymentNumberGenerator.generate(target.getType()),
                memberId,
                target,
                payable.amount(),
                method,
                LocalDateTime.now()
        );
        paymentRepository.save(payment);

        log.info("event=payment.prepared, paymentNumber={}, targetType={}, targetId={}, amount={}",
                payment.getPaymentNumber(), target.getType(), target.getId(), payment.getAmount());

        return PaymentPrepareResponse.of(payment, payable.title());
    }

    /**
     * Orchestrate a Payment Object's Confirmation
     * 1. preempt: Transit the state of a Payment Object to APPROVING
     * 2. approve: Call the PaymentGateway outside without any DB Transaction
     * 3. recordSuccess: Add a PaymentLedger in a new DB Transaction
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse confirm(Long memberId, String paymentNumber, String gatewayToken, Integer amount) {
        PaymentMethod method = preempt(memberId, paymentNumber, amount);
        log.info("event=payment.approval_started, paymentNumber={}, amount={}", paymentNumber, amount);

        GatewayApproval approval = approve(method, paymentNumber, gatewayToken, amount);

        PaymentResponse response = recordSuccess(paymentNumber, approval);

        paymentMetrics.incrementPaymentSuccess();
        log.info("event=payment.approved, paymentNumber={}, transactionId={}, amount={}",
                paymentNumber, approval.transactionId(), amount);

        return response;
    }

    public PaymentResponse getPayment(Long memberId, String paymentNumber) {
        Payment payment = findByPaymentNumber(paymentNumber);
        validateOwner(payment, memberId);
        return PaymentResponse.from(payment);
    }

    //=================================================================================================================

    private PaymentMethod preempt(Long memberId, String paymentNumber, Integer amount) {
        for (int attempt = 1; ; attempt++) {
            try {
                return approvalRecorder.preempt(memberId, paymentNumber, amount);
            } catch (OptimisticLockingFailureException e) {
                boolean lastAttempt = attempt == MAX_PREEMPT_ATTEMPTS;
                log.warn("event=payment.preempt_conflicted, paymentNumber={}, attempt={}/{}, retrying={}",
                        paymentNumber, attempt, MAX_PREEMPT_ATTEMPTS, !lastAttempt);
                if (lastAttempt) {
                    throw new CustomException(ErrorCode.PAYMENT_FAILED, "stock contention on " + paymentNumber);
                }
            }
        }
    }

    private GatewayApproval approve(PaymentMethod method, String paymentNumber, String gatewayToken, Integer amount) {
        try {
            return gateways.get(method).approve(paymentNumber, gatewayToken, amount);
        } catch (PaymentGatewayException e) {
            // Recorded before rethrowing: throwing inside the recorder would roll the failure entry back
            approvalRecorder.recordFailure(paymentNumber, e.getFailureCode(), e.getMessage());
            paymentMetrics.incrementPaymentFailure();
            log.warn("event=payment.approval_rejected, paymentNumber={}, failureCode={}",
                    paymentNumber, e.getFailureCode());
            throw e;
        } catch (RuntimeException e) {
            // Nothing is recorded: the payment stays APPROVING and the sweep settles it against the gateway
            log.error("event=payment.approval_unknown, paymentNumber={}", paymentNumber, e);
            throw new CustomException(ErrorCode.PAYMENT_RESULT_UNKNOWN);
        }
    }

    private PaymentResponse recordSuccess(String paymentNumber, GatewayApproval approval) {
        try {
            return approvalRecorder.recordSuccess(paymentNumber, approval);
        } catch (RuntimeException e) {
            // The gateway approved but the ledger does not know it yet; the transaction id is the key to reconcile
            log.error("event=payment.approval_record_failed, paymentNumber={}, transactionId={}",
                    paymentNumber, approval.transactionId(), e);
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

    private void validateNoPaymentInProgress(PaymentTarget target) {
        if (paymentRepository.existsByTargetAndStatusIn(target, IN_PROGRESS_OR_DONE)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
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
