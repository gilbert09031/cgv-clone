package com.ceos22.cgv_clone.domain.payment.dto.response;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String orderName,
        Integer totalAmount,
        String status,
        String approvedAt
) {
}
