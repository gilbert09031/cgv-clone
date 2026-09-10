package com.ceos22.cgv_clone.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentTargetType {
    ORDER("매점 주문"),
    RESERVATION("영화 예매");

    private final String description;
}
