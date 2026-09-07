package com.ceos22.cgv_clone.common.security;

import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("권한이 부족한 요청 - {} {}", request.getMethod(), request.getRequestURI());
        errorResponseWriter.write(response, ErrorCode.AUTHORIZATION_FAILED);
    }
}
