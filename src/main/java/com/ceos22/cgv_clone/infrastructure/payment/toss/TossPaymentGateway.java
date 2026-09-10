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

    /**
     * paymentNumber를 토스 orderId로, gatewayToken을 paymentKey로 보낸다.
     * TODO : Toss Payments는 orderId의 재사용을 금지하고 있어, Payment 객체를 매 요청마다 새로 생성해야하기에, PaymentLedger 도입을 재검토해야함.
     */
    @Override
    public GatewayApproval approve(String paymentNumber, String gatewayToken, int amount) {
        try {
            TossPaymentResponse response = client.confirmPayment(
                    new TossConfirmRequest(gatewayToken, paymentNumber, amount));
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
