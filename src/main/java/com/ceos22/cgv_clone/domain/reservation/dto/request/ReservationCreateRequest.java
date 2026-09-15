package com.ceos22.cgv_clone.domain.reservation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservationCreateRequest(
        @NotNull(message = "스케쥴 ID는 필수입니다")
        Long scheduleId,

        @NotEmpty(message = "좌석 ID는 필수입니다")
        List<Long> scheduleSeatIds
) {
}
