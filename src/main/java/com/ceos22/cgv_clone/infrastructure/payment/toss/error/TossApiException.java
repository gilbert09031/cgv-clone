package com.ceos22.cgv_clone.infrastructure.payment.toss.error;

import lombok.Getter;

/**
 * 토스가 4xx로 요청을 거절했음을 뜻하는 인프라 내부 예외.
 * 5xx·타임아웃·연결 실패처럼 결과를 알 수 없는 장애는 이 예외로 감싸지 않는다.
 * 도메인으로 나갈 때는 TossPaymentGateway가 PaymentGatewayException으로 바꾼다.
 */
@Getter
public class TossApiException extends RuntimeException {

    private final int httpStatus;
    private final String code;

    public TossApiException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
