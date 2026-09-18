package com.ceos22.cgv_clone.domain.reservation.entity;

public enum SeatStatus {
    AVAILABLE,
    // Held by a PENDING reservation until holdExpiresAt
    HELD,
    RESERVED
}
