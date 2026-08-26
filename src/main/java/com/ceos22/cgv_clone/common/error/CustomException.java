package com.ceos22.cgv_clone.common.error;

import lombok.Getter;

/**
 * ErrorCode를 실어 나르는 애플리케이션 공통 예외.
 * 도메인별 예외는 이 클래스를 상속하며, GlobalExceptionHandler가 단일 지점에서 처리한다.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 응답 메시지는 ErrorCode의 것을 쓰되, 로그에는 더 구체적인 원인을 남기고 싶을 때 사용한다.
     */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
