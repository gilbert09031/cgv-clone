package com.ceos22.cgv_clone.domain.payment.target;

/**
 * 결제 가능하다고 확인된 대상의 요약. 결제 준비 시 금액 확정과 결제창의 orderName에 사용
 */
public record PayableTarget(int amount, String title) {

    public PayableTarget {
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("결제 대상 이름은 필수입니다.");
        }
    }
}
