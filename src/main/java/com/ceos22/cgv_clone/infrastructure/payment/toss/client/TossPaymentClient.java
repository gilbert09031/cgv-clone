package com.ceos22.cgv_clone.infrastructure.payment.toss.client;

import com.ceos22.cgv_clone.infrastructure.payment.toss.dto.request.TossConfirmRequest;
import com.ceos22.cgv_clone.infrastructure.payment.toss.dto.response.TossErrorResponse;
import com.ceos22.cgv_clone.infrastructure.payment.toss.dto.response.TossPaymentResponse;
import com.ceos22.cgv_clone.infrastructure.payment.toss.error.TossApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 토스 API HTTP 호출. 오류 처리 규칙:
 * - 4xx: 토스가 요청을 거절한 것. 오류 본문을 파싱해 {@link TossApiException}으로 던진다.
 * - 5xx, 타임아웃, 연결 실패: 결과를 알 수 없는 장애. WebClient 예외를 그대로 전파한다.
 * 로그는 호출 사실만 DEBUG로 남기고, 실패 로그는 예외를 처리하는 계층에 맡긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    private static final String FALLBACK_ERROR_CODE = "UNPARSEABLE_ERROR";

    private final WebClient tossPaymentWebClient;
    private final ObjectMapper objectMapper;

    public TossPaymentResponse confirmPayment(TossConfirmRequest request) {
        log.debug("event=toss.confirm_requested, orderId={}, paymentKey={}", request.orderId(), request.paymentKey());

        return tossPaymentWebClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toApiException)
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    public TossPaymentResponse cancelPayment(String paymentKey, String cancelReason) {
        log.debug("event=toss.cancel_requested, paymentKey={}", paymentKey);

        return tossPaymentWebClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .bodyValue(new CancelRequest(cancelReason))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toApiException)
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    public TossPaymentResponse getPayment(String paymentKey) {
        log.debug("event=toss.query_requested, paymentKey={}", paymentKey);

        return tossPaymentWebClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toApiException)
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    private Mono<? extends Throwable> toApiException(ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    TossErrorResponse error = parseError(body);
                    return new TossApiException(status, error.code(), error.message());
                });
    }

    private TossErrorResponse parseError(String body) {
        try {
            TossErrorResponse parsed = objectMapper.readValue(body, TossErrorResponse.class);
            if (parsed.code() != null) {
                return parsed;
            }
        } catch (Exception ignored) {
            // 본문이 토스 오류 형식이 아니면 아래 fallback으로 처리한다.
        }
        return new TossErrorResponse(FALLBACK_ERROR_CODE, body);
    }

    private record CancelRequest(String cancelReason) {}
}
