package com.ceos22.cgv_clone.domain.payment.target;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;

/**
 * 결제 도메인이 결제 대상 도메인(주문, 예매 등)에 요구하는 동작
 * 구현체는 각 대상 도메인이 소유하며, 결제 서비스는 supports()로 고른 구현체에 위임
 * 결제 도메인은 대상 엔티티를 직접 참조하지 않음
 */
public interface PaymentTargetHandler {

    /** 이 구현체가 담당하는 대상 종류*/
    PaymentTargetType supports();

    /**
     * 존재 여부, 소유권, 결제 가능 여부를 검증 / amount & orderName을 돌려준다
     * 결제 준비와 승인 선점 시점에 호출됨
     */
    PayableTarget verifyPayable(Long targetId, Long memberId);

    /**
     * 승인이 원장에 기록된 뒤 같은 트랜잭션에서 호출된다. 대상을 확정한다.
     */
    void onApproved(Long targetId);

    /**
     *
     * 취소가 원장에 기록된 뒤 같은 트랜잭션에서 호출된다. 결제시도 이전의 상태로 롤백한다.
     */
    void onCancelled(Long targetId);
}
