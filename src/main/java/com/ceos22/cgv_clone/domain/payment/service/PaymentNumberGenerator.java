package com.ceos22.cgv_clone.domain.payment.service;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generate PaymentNumber
 * Format: {targetPrefix}-yyyyMMdd-{UUID} (Max Length 52 characters, Gateway max-length Constraint 64 characters)
 */
@Component
public class PaymentNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(PaymentTargetType targetType) {
        return String.join("-", targetType.getNumberPrefix(), LocalDate.now().format(DATE_FORMAT), UUID.randomUUID().toString());
    }
}
