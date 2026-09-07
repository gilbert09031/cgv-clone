package com.ceos22.cgv_clone.domain.reservation.controller;

import com.ceos22.cgv_clone.common.error.SuccessCode;
import com.ceos22.cgv_clone.common.response.ApiResponse;
import com.ceos22.cgv_clone.domain.auth.security.CustomUserDetails;
import com.ceos22.cgv_clone.domain.reservation.dto.request.ReservationCreateRequest;
import com.ceos22.cgv_clone.domain.reservation.dto.response.ReservationResponse;
import com.ceos22.cgv_clone.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveSeat(
            @Valid @RequestBody ReservationCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReservationResponse reservation = reservationService.createReservation(userDetails.getMemberId(), request);
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, reservation);
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ReservationResponse> reservations = reservationService.findMyReservations(userDetails.getMemberId());
        return ApiResponse.success(SuccessCode.GET_SUCCESS, reservations);
    }
}
