package com.ceos22.cgv_clone.domain.payment.dto.response;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentLedgerEntry;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentLedgerEntryType;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentStatus;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        String paymentNumber,
        PaymentTargetType targetType,
        Long targetId,
        PaymentMethod method,
        Integer amount,
        PaymentStatus status,
        LocalDateTime requestedAt,
        List<LedgerEntryResponse> ledger
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getTarget().getType(),
                payment.getTarget().getId(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getRequestedAt(),
                payment.getLedger().stream().map(LedgerEntryResponse::from).toList()
        );
    }

    public record LedgerEntryResponse(
            PaymentLedgerEntryType type,
            String gatewayOrderId,
            String transactionId,
            String failureCode,
            String failureReason,
            LocalDateTime occurredAt
    ) {
        static LedgerEntryResponse from(PaymentLedgerEntry entry) {
            return new LedgerEntryResponse(
                    entry.getType(),
                    entry.getGatewayOrderId(),
                    entry.getTransactionId(),
                    entry.getFailureCode(),
                    entry.getFailureReason(),
                    entry.getOccurredAt()
            );
        }
    }
}
