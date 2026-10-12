package com.ceos22.cgv_clone.domain.payment.entity;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_status_requested_at", columnList = "status, requested_at"),
                @Index(name = "idx_payment_target", columnList = "target_type, target_id"),
                @Index(name = "idx_payment_member", columnList = "member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CGV에서 발급하는 결제 번호. 게이트웨이에는 이 번호가 아니라 시도마다 발급하는 orderId가 나간다
    @Column(nullable = false, unique = true, updatable = false)
    private String paymentNumber;

    @Column(nullable = false, updatable = false)
    private Long memberId;

    @Embedded
    private PaymentTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentMethod method;

    @Column(nullable = false, updatable = false)
    private Integer amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.PERSIST)
    @OrderBy("occurredAt ASC, id ASC")
    private List<PaymentLedgerEntry> ledger = new ArrayList<>();

    private Payment(String paymentNumber, Long memberId, PaymentTarget target, Integer amount,
                    PaymentMethod method, LocalDateTime requestedAt) {
        validate(paymentNumber, memberId, target, amount, method, requestedAt);
        this.paymentNumber = paymentNumber;
        this.memberId = memberId;
        this.target = target;
        this.amount = amount;
        this.method = method;
        this.requestedAt = requestedAt;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment createPending(String paymentNumber, Long memberId, PaymentTarget target,
                                        Integer amount, PaymentMethod method, LocalDateTime requestedAt) {
        return new Payment(paymentNumber, memberId, target, amount, method, requestedAt);
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public String startAttempt(LocalDateTime attemptedAt) {
        validateOpen();
        String gatewayOrderId = nextGatewayOrderId();
        append(PaymentLedgerEntry.attempted(this, gatewayOrderId, attemptedAt));
        return gatewayOrderId;
    }

    public void startApproval(String gatewayOrderId, LocalDateTime requestedAt) {
        validateOpen();
        if (!hasAttempt(gatewayOrderId)) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        append(PaymentLedgerEntry.approvalRequested(this, gatewayOrderId, requestedAt));
    }

    public void recordApproval(String transactionId, LocalDateTime approvedAt) {
        validateApproving();
        append(PaymentLedgerEntry.approved(this, approvingOrderId(), transactionId, approvedAt));
    }

    public void recordFailure(String failureCode, String failureReason, LocalDateTime failedAt) {
        validateApproving();
        append(PaymentLedgerEntry.failed(this, approvingOrderId(), failureCode, failureReason, failedAt));
    }

    public void recordCancellation(String transactionId, LocalDateTime cancelledAt) {
        if (status != PaymentStatus.APPROVED) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        append(PaymentLedgerEntry.cancelled(this, latestOrderIdOf(PaymentLedgerEntryType.APPROVED),
                transactionId, cancelledAt));
    }

    // 승인 시도를 시작할 수 있는 상태
    public boolean isOpen() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.FAILED;
    }

    public boolean isApproving() {
        return status == PaymentStatus.APPROVING;
    }

    // 대상이 묶여 있는 상태. 승인 중이거나 이미 결제됨
    public boolean isInProgressOrDone() {
        return status == PaymentStatus.APPROVING || status == PaymentStatus.APPROVED;
    }

    public boolean hasAttempt(String gatewayOrderId) {
        return ledger.stream()
                .filter(entry -> entry.getType() == PaymentLedgerEntryType.ATTEMPTED)
                .anyMatch(entry -> entry.getGatewayOrderId().equals(gatewayOrderId));
    }

    public long failureCount() {
        return ledger.stream()
                .filter(entry -> entry.getType() == PaymentLedgerEntryType.FAILED)
                .count();
    }

    public Optional<String> approvedTransactionId() {
        return ledger.stream()
                .filter(entry -> entry.getType() == PaymentLedgerEntryType.APPROVED)
                .map(PaymentLedgerEntry::getTransactionId)
                .findFirst();
    }

    public boolean hasAmount(Integer amount) {
        return this.amount.equals(amount);
    }

    public boolean hasMethod(PaymentMethod method) {
        return this.method == method;
    }

    public List<PaymentLedgerEntry> getLedger() {
        return Collections.unmodifiableList(ledger);
    }

    // Ledger Append & Payment Status 변경
    private void append(PaymentLedgerEntry entry) {
        ledger.add(entry);
        this.status = entry.getType().toPaymentStatus();
    }

    private String nextGatewayOrderId() {
        long attempts = ledger.stream()
                .filter(entry -> entry.getType() == PaymentLedgerEntryType.ATTEMPTED)
                .count();
        return paymentNumber + "-" + (attempts + 1);
    }

    private String approvingOrderId() {
        return latestOrderIdOf(PaymentLedgerEntryType.APPROVAL_REQUESTED);
    }

    private String latestOrderIdOf(PaymentLedgerEntryType type) {
        return ledger.stream()
                .filter(entry -> entry.getType() == type)
                .reduce((first, second) -> second)
                .map(PaymentLedgerEntry::getGatewayOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_PAYMENT_STATUS));
    }

    private void validateOpen() {
        if (!isOpen()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void validateApproving() {
        if (!isApproving()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void validate(String paymentNumber, Long memberId, PaymentTarget target, Integer amount,
                          PaymentMethod method, LocalDateTime requestedAt) {
        if (paymentNumber == null || paymentNumber.isBlank()) {
            throw new IllegalArgumentException("결제 번호는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("결제 회원은 필수입니다.");
        }
        if (target == null) {
            throw new IllegalArgumentException("결제 대상은 필수입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        }
        if (method == null) {
            throw new IllegalArgumentException("결제 수단은 필수입니다.");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("결제 요청 시각은 필수입니다.");
        }
    }
}
