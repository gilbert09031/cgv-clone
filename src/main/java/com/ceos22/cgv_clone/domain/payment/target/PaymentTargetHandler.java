package com.ceos22.cgv_clone.domain.payment.target;

import com.ceos22.cgv_clone.domain.payment.entity.PaymentTargetType;

/**
 * Operations the payment domain requires from a payment target domain (Order, Reservation)
 * Implemented by each target domain; the payment service only delegates by supports()
 */
public interface PaymentTargetHandler {

    /** Target type this implementation handles */
    PaymentTargetType supports();

    /**
     * Soft check at Payment Prepare Stage: existence, ownership, payable state. Reserves nothing
     * @return amount to charge and the title shown in the payment widget
     */
    PayableTarget verifyPayable(Long targetId, Long memberId);

    /**
     * Called in the preemption transaction, before the gateway is asked to approve.
     * Reserves what the target needs to be fulfilled (e.g. stock). Throwing aborts the approval before any money moves
     */
    void onApprovalStarted(Long targetId, String paymentNumber, String gatewayOrderId);

    /** Called after the approval is recorded. Only a state transition; must not fail for business reasons */
    void onApproved(Long targetId, String paymentNumber, String gatewayOrderId);

    /** Called after a gateway rejection is recorded. Releases what onApprovalStarted reserved */
    void onApprovalFailed(Long targetId);

    /** Called after the cancellation is recorded.
     * Releases the Ownership of a Target and reverts the TargetState
     */
    void onCancelled(Long targetId);
}
