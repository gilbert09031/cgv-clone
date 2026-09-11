package com.ceos22.cgv_clone.domain.payment.gateway;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentGatewayException extends CustomException {

    /** 게이트웨이가 준 실패 코드. 원장의 failureCode에 기록 */
    private final String failureCode;

    public PaymentGatewayException(String failureCode, String message) {
        super(ErrorCode.PAYMENT_FAILED, message);
        this.failureCode = failureCode;
    }
}
