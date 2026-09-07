package com.ceos22.cgv_clone.domain.auth.jwt.error;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;

/**
 * 토큰 검증 단계에서 발생하는 예외.
 * CustomException을 상속하므로 GlobalExceptionHandler의 CustomException 핸들러가 그대로 처리하고,
 * JwtAuthenticationFilter에서는 다른 CustomException과 구분해 catch할 수 있다.
 */
public class JwtException extends CustomException {

    public JwtException(ErrorCode errorCode) {
        super(errorCode);
    }

    public JwtException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
