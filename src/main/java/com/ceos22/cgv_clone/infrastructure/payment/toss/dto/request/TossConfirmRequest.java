package com.ceos22.cgv_clone.infrastructure.payment.toss.dto.request;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
