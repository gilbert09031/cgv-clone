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

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.atDebug()
                .addKeyValue("event", "auth.authorization_rejected")
                .addKeyValue("errorCode", ErrorCode.AUTHORIZATION_FAILED.getCode())
                .addKeyValue("statusCode", ErrorCode.AUTHORIZATION_FAILED.getStatus().value())
                .log();
        errorResponseWriter.write(response, ErrorCode.AUTHORIZATION_FAILED);
    }
}
