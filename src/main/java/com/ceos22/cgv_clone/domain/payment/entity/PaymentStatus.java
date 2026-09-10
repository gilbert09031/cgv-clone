package com.ceos22.cgv_clone.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("결제 대기"),
    APPROVING("승인 진행 중"),
    APPROVED("승인 완료"),
    FAILED("승인 실패"),
    CANCELLED("취소 완료");

    private final String description;
}
