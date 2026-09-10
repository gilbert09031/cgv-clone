package com.ceos22.cgv_clone.infrastructure.payment.toss.dto.response;

public record TossErrorResponse(
        String code,
        String message
) {
}
