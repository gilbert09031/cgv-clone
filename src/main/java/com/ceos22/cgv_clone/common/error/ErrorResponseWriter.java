package com.ceos22.cgv_clone.common.error;

import com.ceos22.cgv_clone.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security Filter Chain에서 ErrorResponse를 조립
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        // 이미 응답이 나가기 시작한 뒤에 다시 쓰면 IllegalStateException이 발생한다.
        if (response.isCommitted()) {
            log.warn("응답이 이미 커밋되어 에러 응답을 작성하지 못했습니다. errorCode={}", errorCode.getCode());
            return;
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.errorBody(errorCode));
    }
}
