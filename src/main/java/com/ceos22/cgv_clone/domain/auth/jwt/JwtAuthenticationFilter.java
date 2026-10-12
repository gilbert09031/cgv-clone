package com.ceos22.cgv_clone.domain.auth.jwt;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.common.error.ErrorResponseWriter;
import com.ceos22.cgv_clone.domain.auth.jwt.error.BlacklistedTokenException;
import com.ceos22.cgv_clone.domain.auth.jwt.error.JwtException;
import com.ceos22.cgv_clone.domain.auth.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "BL:";

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            authenticate(request);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();

            log.atDebug()
                    .addKeyValue("event", "auth.authentication_rejected")
                    .addKeyValue("errorCode", e.getErrorCode().getCode())
                    .addKeyValue("statusCode", e.getErrorCode().getStatus().value())
                    .log();

            errorResponseWriter.write(response, e.getErrorCode());
            return;
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();

            log.atDebug()
                    .addKeyValue("event", "auth.authentication_rejected")
                    .addKeyValue("errorCode", e.getErrorCode().getCode())
                    .addKeyValue("statusCode", ErrorCode.AUTHENTICATION_FAILED.getStatus().value())
                    .log();

            errorResponseWriter.write(response, ErrorCode.AUTHENTICATION_FAILED);
            return;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();

            log.atError()
                    .addKeyValue("event", "api.request.errored")
                    .addKeyValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                    .setCause(e)
                    .log();

            errorResponseWriter.write(response, ErrorCode.INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String token = jwtTokenProvider.getAccessToken(request);

        // 토큰이 없으면 인증하지 않고 통과시킨다.
        // 이 요청에 인증이 필요한지는 뒤의 AuthorizationFilter가 판단하며,
        // 필요한 경우 CustomAuthenticationEntryPoint가 401을 응답한다.
        if (token == null || token.isEmpty()) {
            return;
        }

        if (redisService.hasKey(BLACKLIST_PREFIX + token)) {
            throw new BlacklistedTokenException();
        }

        jwtTokenProvider.validateToken(token);

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
