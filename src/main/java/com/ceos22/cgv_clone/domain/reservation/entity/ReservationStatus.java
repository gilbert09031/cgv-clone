package com.ceos22.cgv_clone.domain.reservation.entity;

// Watched is not a status: it follows from schedule.startTime
public enum ReservationStatus {
    PENDING("결제 대기"),
    CONFIRMED("예매 완료"),
    CANCELED("예매 취소"),
    EXPIRED("결제 시간 만료");

    private final String status;

    ReservationStatus(String status) {
        this.status = status;
    }

    public String Status() {
        return status;
    }
}
