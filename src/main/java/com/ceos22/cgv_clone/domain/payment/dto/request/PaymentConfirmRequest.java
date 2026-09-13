package com.ceos22.cgv_clone.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Values the client holds after the payment widget closes
 * @param gatewayOrderId order id received at preparation and used in the widget
 * @param gatewayToken opaque value the widget returned; Toss calls it paymentKey
 */
public record PaymentConfirmRequest(
        @NotBlank(message = "결제 주문 번호는 필수입니다")
        String gatewayOrderId,

        @NotBlank(message = "결제 토큰은 필수입니다")
        String gatewayToken,

        @NotNull(message = "결제 금액은 필수입니다")
        @Positive(message = "결제 금액은 양수여야 합니다")
        Integer amount
) {
}
