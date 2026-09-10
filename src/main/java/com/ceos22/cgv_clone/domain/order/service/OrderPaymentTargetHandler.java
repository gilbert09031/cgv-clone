package com.ceos22.cgv_clone.domain.order.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.order.entity.Order;
import com.ceos22.cgv_clone.domain.order.repository.OrderRepository;
import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;
import com.ceos22.cgv_clone.domain.payment.target.PayableTarget;
import com.ceos22.cgv_clone.domain.payment.target.PaymentTargetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 매점 주문에 대한 PaymentTargetHandler 구현.
 * 결제 서비스의 트랜잭션 안에서 호출되므로 별도 트랜잭션을 열지 않고, 상태 변경은 OrderService에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class OrderPaymentTargetHandler implements PaymentTargetHandler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    public PaymentTargetType supports() {
        return PaymentTargetType.ORDER;
    }

    @Override
    public PayableTarget verifyPayable(Long targetId, Long memberId) {
        Order order = orderRepository.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_FAILED);
        }
        if (!order.isPending()) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        return new PayableTarget(order.getTotalPrice(), order.summaryName());
    }

    @Override
    public void onApproved(Long targetId) {
        orderService.confirmOrderAfterPayment(targetId);
    }

    @Override
    public void onCancelled(Long targetId) {
        orderService.cancelOrderAfterPaymentCancel(targetId);
    }
}
