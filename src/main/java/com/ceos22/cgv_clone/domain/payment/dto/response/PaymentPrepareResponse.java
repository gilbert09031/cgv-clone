package com.ceos22.cgv_clone.domain.payment.dto.response;

import com.ceos22.cgv_clone.domain.payment.entity.Payment;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;

/**
 * Used for Client Side Payment Info Rendering
 * @param gatewayOrderId order id for this attempt; the widget uses it and the confirm request sends it back
 * @param paymentNumber internal number of the payment the attempt belongs to
 */
public record PaymentPrepareResponse(
        String paymentNumber,
        String gatewayOrderId,
        PaymentMethod method,
        Integer amount,
        String title
) {
    public static PaymentPrepareResponse of(Payment payment, String gatewayOrderId, String title) {
        return new PaymentPrepareResponse(
                payment.getPaymentNumber(),
                gatewayOrderId,
                payment.getMethod(),
                payment.getAmount(),
                title
        );
    }
}
