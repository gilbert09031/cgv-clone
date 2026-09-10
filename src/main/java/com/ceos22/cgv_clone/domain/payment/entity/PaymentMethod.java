package com.ceos22.cgv_clone.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    TOSS_PAYMENTS("토스페이먼츠");

    private final String description;
}
