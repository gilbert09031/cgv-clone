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
 * PaymentTargetHandler for store orders.
 * Runs inside the payment service's transactions;
 * state changes are delegated to OrderService
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

    // ORDER policy: nothing is reserved at preparation; stock is taken here, before the Payment Gateway Confirmation
    @Override
    public void onApprovalStarted(Long targetId) {
        orderService.holdStockForPayment(targetId);
    }

    @Override
    public void onApproved(Long targetId) {
        orderService.confirmOrderAfterPayment(targetId);
    }

    @Override
    public void onApprovalFailed(Long targetId) {
        orderService.releaseStockAfterPaymentFailure(targetId);
    }

    @Override
    public void onCancelled(Long targetId) {
        orderService.cancelOrderAfterPaymentCancel(targetId);
    }
}
