package com.ceos22.cgv_clone.domain.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A person's role in a movie. A director who also acts has one row per role */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_movie_credit_role", columnNames = {"movie_id", "person_id", "role"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movieCreditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditRole role;

    // Only for ACTOR
    private String characterName;

    // Order shown on the movie detail page, ascending
    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    public MovieCredit(Movie movie, Person person, CreditRole role, String characterName, Integer displayOrder) {
        validate(movie, person, role, characterName, displayOrder);
        this.movie = movie;
        this.person = person;
        this.role = role;
        this.characterName = characterName;
        this.displayOrder = displayOrder;
    }

    private void validate(Movie movie, Person person, CreditRole role, String characterName, Integer displayOrder) {
        if (movie == null || person == null || role == null || displayOrder == null) {
            throw new IllegalArgumentException("영화, 인물, 역할, 표시 순서는 필수입니다.");
        }
        if (role != CreditRole.ACTOR && characterName != null) {
            throw new IllegalArgumentException("배역명은 배우에게만 지정할 수 있습니다.");
        }
    }
}
