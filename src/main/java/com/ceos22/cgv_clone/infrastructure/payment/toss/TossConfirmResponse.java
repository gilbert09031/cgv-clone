package com.ceos22.cgv_clone.infrastructure.payment.toss;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String orderName,
        Integer totalAmount,
        String status,
        String approvedAt
) {
}
