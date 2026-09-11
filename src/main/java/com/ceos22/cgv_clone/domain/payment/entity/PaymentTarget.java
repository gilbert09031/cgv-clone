package com.ceos22.cgv_clone.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentTarget {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false)
    private PaymentTargetType type;

    @Column(name = "target_id", nullable = false, updatable = false)
    private Long id;

    private PaymentTarget(PaymentTargetType type, Long id) {
        validate(type, id);
        this.type = type;
        this.id = id;
    }

    public static PaymentTarget of(PaymentTargetType type, Long id) {
        return new PaymentTarget(type, id);
    }

    private void validate(PaymentTargetType type, Long id) {
        if (type == null) {
            throw new IllegalArgumentException("결제 대상 종류는 필수입니다.");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("결제 대상 id는 양수여야 합니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof PaymentTarget other &&
                type == other.type &&
                Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }
}
