package com.ceos22.cgv_clone.domain.movie.dto.response;

import com.ceos22.cgv_clone.domain.movie.entity.Person;

import java.time.LocalDate;

public record PersonResponse(
        Long personId,
        String name,
        LocalDate birthDate,
        String profileImageUrl
) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getPersonId(),
                person.getName(),
                person.getBirthDate(),
                person.getProfileImageUrl()
        );
    }
}
