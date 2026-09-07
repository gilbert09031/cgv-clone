package com.ceos22.cgv_clone.domain.order.controller;

import com.ceos22.cgv_clone.common.error.SuccessCode;
import com.ceos22.cgv_clone.common.response.ApiResponse;
import com.ceos22.cgv_clone.domain.auth.security.CustomUserDetails;
import com.ceos22.cgv_clone.domain.order.dto.request.CartItemRequest;
import com.ceos22.cgv_clone.domain.order.dto.request.CartItemUpdateRequest;
import com.ceos22.cgv_clone.domain.order.dto.response.CartResponse;
import com.ceos22.cgv_clone.domain.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @PathVariable Long storeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse cart = cartService.getCart(userDetails.getMemberId(), storeId);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, cart);
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse cart = cartService.addToCart(userDetails.getMemberId(), request);
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, cart);
    }

    @PatchMapping("/{storeId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @Valid @RequestBody CartItemUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse cart = cartService.updateCartItem(userDetails.getMemberId(), storeId, productId, request.quantity());
        return ApiResponse.success(SuccessCode.UPDATE_SUCCESS, cart);
    }

    @DeleteMapping("/{storeId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse cart = cartService.removeFromCart(userDetails.getMemberId(), storeId, productId);
        return ApiResponse.success(SuccessCode.DELETE_SUCCESS, cart);
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable Long storeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        cartService.clearCart(userDetails.getMemberId(), storeId);
        return ApiResponse.success(SuccessCode.DELETE_SUCCESS);
    }
}