package com.ceos22.cgv_clone.domain.payment.controller;

import com.ceos22.cgv_clone.common.error.SuccessCode;
import com.ceos22.cgv_clone.common.response.ApiResponse;
import com.ceos22.cgv_clone.domain.auth.security.CustomUserDetails;
import com.ceos22.cgv_clone.domain.payment.dto.request.PaymentPrepareRequest;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentPrepareResponse;
import com.ceos22.cgv_clone.domain.payment.dto.response.PaymentResponse;
import com.ceos22.cgv_clone.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Creating a payment is the preparation step: the response carries what the client needs to open the widget
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @Valid @RequestBody PaymentPrepareRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentPrepareResponse response = paymentService.prepare(
                userDetails.getMemberId(), request.toTarget(), request.method());
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, response);
    }

    @GetMapping("/{paymentNumber}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable String paymentNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentResponse response = paymentService.getPayment(userDetails.getMemberId(), paymentNumber);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, response);
    }
}
