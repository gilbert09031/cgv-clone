package com.ceos22.cgv_clone.infrastructure.payment.toss;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
