package com.ceos22.cgv_clone.infrastructure.payment.toss;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class TossWebClientConfig {

    private final WebClient.Builder webClientBuilder;
    private final TossPaymentConfig tossPaymentConfig;

    @Bean
    public WebClient tossPaymentWebClient() {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((tossPaymentConfig.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return webClientBuilder
                .baseUrl(tossPaymentConfig.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
