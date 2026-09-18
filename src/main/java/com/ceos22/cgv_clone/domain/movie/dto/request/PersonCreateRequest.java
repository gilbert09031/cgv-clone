package com.ceos22.cgv_clone.domain.movie.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PersonCreateRequest(
        @NotBlank(message = "인물 이름은 필수입니다.")
        String name,

        LocalDate birthDate,

        String profileImageUrl
) {
}
