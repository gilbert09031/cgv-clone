package com.ceos22.cgv_clone.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentTargetType {
    ORDER("매점 주문", "STORE"),
    RESERVATION("영화 예매", "TICKET");

    private final String description;

    // Used for PaymentNumber prefix
    private final String numberPrefix;
}
