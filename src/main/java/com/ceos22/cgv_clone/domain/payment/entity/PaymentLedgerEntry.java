package com.ceos22.cgv_clone.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "payment_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentLedgerEntryType type;

    // 외부 시스템이 발급한 식별자
    // e.g. Toss Payments : Payment.paymentKey
    @Column(unique = true)
    private String transactionId;

    // 승인은 양수, 취소는 음수, 실패는 0
    @Column(nullable = false)
    private Integer amount;

    private String failureCode;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private PaymentLedgerEntry(Payment payment, PaymentLedgerEntryType type, String transactionId,
                               Integer amount, String failureCode, String failureReason,
                               LocalDateTime occurredAt) {
        if (payment == null) throw new IllegalArgumentException("원장 행은 결제에 속해야 합니다.");
        if (occurredAt == null) throw new IllegalArgumentException("사건 발생 시각은 필수입니다.");
        this.payment = payment;
        this.type = type;
        this.transactionId = transactionId;
        this.amount = amount;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.occurredAt = occurredAt;
    }

    static PaymentLedgerEntry approvalRequested(Payment payment, LocalDateTime requestedAt) {
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.APPROVAL_REQUESTED,
                null, 0, null, null, requestedAt);
    }

    static PaymentLedgerEntry approved(Payment payment, String transactionId,
                                       Integer amount, LocalDateTime approvedAt) {
        requireTransactionId(transactionId);
        requirePositive(amount);
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.APPROVED,
                transactionId, amount, null, null, approvedAt);
    }

    static PaymentLedgerEntry failed(Payment payment, String failureCode,
                                     String failureReason, LocalDateTime failedAt) {
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("실패 원장에는 실패 코드가 필요합니다.");
        }
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.FAILED,
                null, 0, failureCode, failureReason, failedAt);
    }

    static PaymentLedgerEntry cancelled(Payment payment, String transactionId,
                                        Integer amount, LocalDateTime cancelledAt) {
        requireTransactionId(transactionId);
        requirePositive(amount);
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.CANCELLED,
                transactionId, -amount, null, null, cancelledAt);
    }

    private static void requireTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("외부 거래 식별자가 필요합니다.");
        }
    }

    private static void requirePositive(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 양수여야 합니다.");
        }
    }
}
