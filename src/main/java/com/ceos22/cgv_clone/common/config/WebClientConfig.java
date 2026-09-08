package com.ceos22.cgv_clone.common.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public WebClientCustomizer timeoutWebClientCustomizer() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT);

        return builder -> builder.clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
