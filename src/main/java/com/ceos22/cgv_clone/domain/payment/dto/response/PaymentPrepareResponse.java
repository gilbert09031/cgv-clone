package com.ceos22.cgv_clone.domain.payment.dto.response;

import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;

/**
 * Used for Client Side Payment Info Rendering
 */
public record PaymentPrepareResponse(
        String paymentNumber,
        PaymentMethod method,
        Integer amount,
        String title
) {
    public static PaymentPrepareResponse of(Payment payment, String title) {
        return new PaymentPrepareResponse(
                payment.getPaymentNumber(),
                payment.getMethod(),
                payment.getAmount(),
                title
        );
    }
}
