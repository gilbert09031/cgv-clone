package com.ceos22.cgv_clone.common.security;

import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        ErrorCode errorCode = (authorizationHeader == null || authorizationHeader.isBlank())
                ? ErrorCode.MISSING_TOKEN
                : ErrorCode.INVALID_TOKEN;

        log.warn("인증되지 않은 요청 [{}] - {} {}",
                errorCode.getCode(), request.getMethod(), request.getRequestURI());
        errorResponseWriter.write(response, errorCode);
    }
}
