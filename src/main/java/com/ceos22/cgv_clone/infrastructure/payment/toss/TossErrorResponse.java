package com.ceos22.cgv_clone.infrastructure.payment.toss;

public record TossErrorResponse(
        String code,
        String message
) {
}
