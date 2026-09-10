package com.ceos22.cgv_clone.domain.payment.gateway;

import java.time.LocalDateTime;

public record GatewayCancellation(String transactionId, LocalDateTime cancelledAt) {

    public GatewayCancellation {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("취소 거래 식별자는 필수입니다.");
        }
        if (cancelledAt == null) {
            throw new IllegalArgumentException("취소 시각은 필수입니다.");
        }
    }
}
