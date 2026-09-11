package com.ceos22.cgv_clone.domain.payment.dto.request;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTarget;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
        @NotNull(message = "결제 대상 종류는 필수입니다")
        PaymentTargetType targetType,

        @NotNull(message = "결제 대상 ID는 필수입니다")
        Long targetId,

        @NotNull(message = "결제 수단은 필수입니다")
        PaymentMethod method
) {
    public PaymentTarget toTarget() {
        return PaymentTarget.of(targetType, targetId);
    }
}
