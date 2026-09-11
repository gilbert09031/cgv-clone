package com.ceos22.cgv_clone.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentLedgerEntryType {
    APPROVAL_REQUESTED("승인 요청", PaymentStatus.APPROVING),
    APPROVED("승인", PaymentStatus.APPROVED),
    FAILED("승인 실패", PaymentStatus.FAILED),
    CANCELLED("취소", PaymentStatus.CANCELLED);

    private final String description;
    private final PaymentStatus resultingStatus;

    public PaymentStatus toPaymentStatus() {
        return resultingStatus;
    }
}
