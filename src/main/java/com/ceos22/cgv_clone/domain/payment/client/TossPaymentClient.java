package com.ceos22.cgv_clone.domain.payment.client;

import com.ceos22.cgv_clone.domain.payment.dto.request.TossConfirmRequest;
import com.ceos22.cgv_clone.domain.payment.dto.response.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    private final WebClient tossPaymentWebClient;

    public TossConfirmResponse confirmPayment(TossConfirmRequest request) {
        log.debug("event=payment.confirm_requested, orderId={}, paymentKey={}", request.orderId(), request.paymentKey());

        return tossPaymentWebClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    RuntimeException e = new RuntimeException("Toss 결제 승인 실패: " + body);
                                    log.error("event=payment.confirm_failed, orderId={}, paymentKey={}", request.orderId(), request.paymentKey(), e);
                                    return Mono.error(e);
                                })
                )
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }

    public void cancelPayment(String paymentKey, String cancelReason) {
        log.debug("event=payment.cancel_requested, paymentKey={}", paymentKey);

        tossPaymentWebClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .bodyValue(new CancelRequest(cancelReason))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    RuntimeException e = new RuntimeException("Toss 결제 취소 실패: " + body);
                                    log.error("event=payment.cancel_failed, paymentKey={}", paymentKey, e);
                                    return Mono.error(e);
                                })
                )
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }

    public TossConfirmResponse getPayment(String paymentKey) {
        log.debug("event=payment.query_requested, paymentKey={}", paymentKey);

        return tossPaymentWebClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    RuntimeException e = new RuntimeException("Toss 결제 조회 실패: " + body);
                                    log.error("event=payment.query_failed, paymentKey={}", paymentKey, e);
                                    return Mono.error(e);
                                })
                )
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }

    private record CancelRequest(String cancelReason) {}
}
