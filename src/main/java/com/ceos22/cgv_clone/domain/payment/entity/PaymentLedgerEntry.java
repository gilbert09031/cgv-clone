package com.ceos22.cgv_clone.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "payment_ledger", indexes = @Index(name = "idx_ledger_gateway_order_id", columnList = "gateway_order_id"))
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

    @Column(name = "gateway_order_id", nullable = false, updatable = false)
    private String gatewayOrderId;

    // 외부 시스템이 발급한 식별자
    // e.g. Toss Payments : Payment.paymentKey
    @Column(unique = true)
    private String transactionId;

    private String failureCode;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private PaymentLedgerEntry(Payment payment, PaymentLedgerEntryType type, String gatewayOrderId,
                               String transactionId, String failureCode, String failureReason,
                               LocalDateTime occurredAt) {
        if (payment == null) throw new IllegalArgumentException("원장 행은 결제에 속해야 합니다.");
        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            throw new IllegalArgumentException("원장 행에는 시도의 주문 번호가 필요합니다.");
        }
        if (occurredAt == null) throw new IllegalArgumentException("사건 발생 시각은 필수입니다.");
        this.payment = payment;
        this.type = type;
        this.gatewayOrderId = gatewayOrderId;
        this.transactionId = transactionId;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.occurredAt = occurredAt;
    }

    static PaymentLedgerEntry attempted(Payment payment, String gatewayOrderId, LocalDateTime attemptedAt) {
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.ATTEMPTED,
                gatewayOrderId, null, null, null, attemptedAt);
    }

    static PaymentLedgerEntry approvalRequested(Payment payment, String gatewayOrderId, LocalDateTime requestedAt) {
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.APPROVAL_REQUESTED,
                gatewayOrderId, null, null, null, requestedAt);
    }

    static PaymentLedgerEntry approved(Payment payment, String gatewayOrderId, String transactionId,
                                       LocalDateTime approvedAt) {
        requireTransactionId(transactionId);
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.APPROVED,
                gatewayOrderId, transactionId, null, null, approvedAt);
    }

    static PaymentLedgerEntry failed(Payment payment, String gatewayOrderId, String failureCode,
                                     String failureReason, LocalDateTime failedAt) {
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("실패 원장에는 실패 코드가 필요합니다.");
        }
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.FAILED,
                gatewayOrderId, null, failureCode, failureReason, failedAt);
    }

    static PaymentLedgerEntry cancelled(Payment payment, String gatewayOrderId, String transactionId,
                                        LocalDateTime cancelledAt) {
        requireTransactionId(transactionId);
        return new PaymentLedgerEntry(payment, PaymentLedgerEntryType.CANCELLED,
                gatewayOrderId, transactionId, null, null, cancelledAt);
    }

    private static void requireTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("외부 거래 식별자가 필요합니다.");
        }
    }
}
