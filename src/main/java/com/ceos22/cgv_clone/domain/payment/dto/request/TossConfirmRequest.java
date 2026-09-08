package com.ceos22.cgv_clone.domain.payment.dto.request;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
