package com.ceos22.cgv_clone.domain.payment.repository;

import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    List<Payment> findAllByStatusInAndRequestedAtBefore(Collection<PaymentStatus> statuses, LocalDateTime cutoff);

    @Query("SELECT DISTINCT e.payment.target FROM PaymentLedgerEntry e WHERE e.gatewayOrderId = :gatewayOrderId")
    Optional<PaymentTarget> findTargetByGatewayOrderId(@Param("gatewayOrderId") String gatewayOrderId);

    /**
     * Lock and return every payment of one target, held until the transaction ends.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.target = :target")
    List<Payment> lockAllByTarget(@Param("target") PaymentTarget target);
}
