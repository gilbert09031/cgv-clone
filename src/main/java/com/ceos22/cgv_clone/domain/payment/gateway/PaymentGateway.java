package com.ceos22.cgv_clone.domain.payment.gateway;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentMethod;

/**
 * 결제 도메인이 외부 결제 게이트웨이에 요구하는 동작.
 * */
public interface PaymentGateway {

    /** 이 구현체가 담당하는 결제 수단. 구현체가 여럿일 때 Payment.method로 고르는 기준. */
    PaymentMethod method();

    /**
     * 승인 요청.
     *
     * @param paymentNumber CGV가 발급한 결제 번호. 게이트웨이에는 주문 식별자로 전달된다.
     * @param gatewayToken  클라이언트가 결제 위젯에서 받아 넘긴 값. 도메인은 내용을 해석하거나 저장하지 않는다.
     * @param amount        CGV가 기록한 결제 금액. 게이트웨이 쪽 금액과 일치해야 승인된다.
     * @return 게이트웨이가 발급한 거래 식별자와 승인 시각
     * @throws PaymentGatewayException 게이트웨이가 승인을 명확히 거절한 경우.
     *                                 결과를 알 수 없는 장애(타임아웃 등)는 이 예외로 감싸지 않고 그대로 전파한다.
     */
    GatewayApproval approve(String paymentNumber, String gatewayToken, int amount);

    /**
     * 승인된 거래의 취소.
     *
     * @param transactionId 승인 시 받은 거래 식별자
     * @param reason        취소 사유
     * @return 취소 거래의 식별자와 취소 시각
     * @throws PaymentGatewayException 게이트웨이가 취소를 명확히 거절한 경우
     */
    GatewayCancellation cancel(String transactionId, String reason);
}
