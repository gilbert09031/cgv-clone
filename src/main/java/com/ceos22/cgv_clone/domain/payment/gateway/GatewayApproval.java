package com.ceos22.cgv_clone.domain.payment.gateway;

import java.time.LocalDateTime;

public record GatewayApproval(String transactionId, LocalDateTime approvedAt) {

    public GatewayApproval {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("거래 식별자는 필수입니다.");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("승인 시각은 필수입니다.");
        }
    }
}
