package com.ceos22.cgv_clone.domain.payment.target;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** PaymentTargetHandler implementations keyed by the target type they support */
@Component
public class PaymentTargetHandlers {

    private final Map<PaymentTargetType, PaymentTargetHandler> handlers;

    public PaymentTargetHandlers(List<PaymentTargetHandler> handlers) {
        // Duplicate handlers for one target type fail at startup
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        handler -> handler.supports(),
                        handler -> handler
                ));
    }

    public PaymentTargetHandler get(PaymentTargetType type) {
        PaymentTargetHandler handler = handlers.get(type);
        if (handler == null) {
            throw new CustomException(ErrorCode.UNSUPPORTED_PAYMENT_TARGET);
        }
        return handler;
    }
}
