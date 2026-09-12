package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentPrepareResponse;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.repository.PaymentRepository;
import com.ceos22.cgv_clone.domain.payment.target.PayableTarget;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    // PENDING and FAILED do not block: failures inside the widget never reach the server and stay PENDING
    private static final List<PaymentStatus> IN_PROGRESS_OR_DONE =
            List.of(PaymentStatus.APPROVING, PaymentStatus.APPROVED);

    private final PaymentRepository paymentRepository;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final PaymentTargetHandlers targetHandlers;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentNumberGenerator paymentNumberGenerator,
                          PaymentTargetHandlers targetHandlers) {
        this.paymentRepository = paymentRepository;
        this.paymentNumberGenerator = paymentNumberGenerator;
        this.targetHandlers = targetHandlers;
    }

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

    public PaymentResponse getPayment(Long memberId, String paymentNumber) {
        Payment payment = findByPaymentNumber(paymentNumber);
        validateOwner(payment, memberId);
        return PaymentResponse.from(payment);
    }

    //=================================================================================================================

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
