package com.ceos22.cgv_clone.infrastructure.payment.toss;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;
import com.ceos22.cgv_clone.domain.payment.gateway.GatewayApproval;
import com.ceos22.cgv_clone.domain.payment.gateway.GatewayCancellation;
import com.ceos22.cgv_clone.domain.payment.gateway.PaymentGateway;
import com.ceos22.cgv_clone.domain.payment.gateway.PaymentGatewayException;
import com.ceos22.cgv_clone.infrastructure.payment.toss.client.TossPaymentClient;
import com.ceos22.cgv_clone.infrastructure.payment.toss.dto.request.TossConfirmRequest;
import com.ceos22.cgv_clone.infrastructure.payment.toss.dto.response.TossPaymentResponse;
import com.ceos22.cgv_clone.infrastructure.payment.toss.error.TossApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PaymentGateway의 Toss Payments 구현체
 */
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private final TossPaymentClient client;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.TOSS_PAYMENTS;
    }

    // 시도마다 발급된 gatewayOrderId를 토스 orderId로, gatewayToken을 paymentKey로 보낸다.
    // 토스는 orderId 재사용을 DUPLICATED_ORDER_ID로 거절한다 (2026-09-11 확인, 개발자 커뮤니티)
    @Override
    public GatewayApproval approve(String gatewayOrderId, String gatewayToken, int amount) {
        try {
            TossPaymentResponse response = client.confirmPayment(
                    new TossConfirmRequest(gatewayToken, gatewayOrderId, amount));
            return new GatewayApproval(response.paymentKey(), response.approvedLocalDateTime());
        } catch (TossApiException e) {
            throw new PaymentGatewayException(e.getCode(), e.getMessage());
        }
    }

    @Override
    public GatewayCancellation cancel(String transactionId, String reason) {
        try {
            TossPaymentResponse response = client.cancelPayment(transactionId, reason);
            TossPaymentResponse.Cancel cancel = response.latestCancel();
            return new GatewayCancellation(cancel.transactionKey(), cancel.canceledLocalDateTime());
        } catch (TossApiException e) {
            throw new PaymentGatewayException(e.getCode(), e.getMessage());
        }
    }
}
