package com.ceos22.cgv_clone.domain.payment.repository;

import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    List<Payment> findAllByTargetOrderByRequestedAtDesc(PaymentTarget target);

    List<Payment> findAllByStatusInAndRequestedAtBefore(Collection<PaymentStatus> statuses, LocalDateTime cutoff);
}
