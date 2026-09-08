package com.ceos22.cgv_clone.domain.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment.toss")
@Getter
@Setter
public class TossPaymentConfig {

    private String baseUrl;
    private String secretKey;
}
