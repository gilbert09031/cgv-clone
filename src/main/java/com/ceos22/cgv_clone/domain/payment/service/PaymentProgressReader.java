package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import com.ceos22.cgv_clone.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only payment state for target domains.
 * Depends only on PaymentRepository: target services are dependencies of PaymentTargetHandler,
 * so depending on PaymentService from them would form a bean cycle
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentProgressReader {

    private final PaymentRepository paymentRepository;

    /**
     * Whether the gateway may be approving a payment of the target right now
     * @return {@code true} while a payment is {@code APPROVING}, including an undecided outcome left for the sweep
     */
    public boolean hasApprovalInProgress(PaymentTargetType targetType, Long targetId) {
        return paymentRepository.existsByTargetAndStatus(PaymentTarget.of(targetType, targetId), PaymentStatus.APPROVING);
    }
}
