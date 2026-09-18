package com.ceos22.cgv_clone.domain.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Film maker or cast member. One row per real person, whatever roles they take across movies */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long personId;

    @Column(nullable = false)
    private String name;

    private LocalDate birthDate;

    private String profileImageUrl;

    @Builder
    public Person(String name, LocalDate birthDate, String profileImageUrl) {
        this.name = name;
        this.birthDate = birthDate;
        this.profileImageUrl = profileImageUrl;
    }
}
