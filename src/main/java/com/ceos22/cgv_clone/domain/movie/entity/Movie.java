package com.ceos22.cgv_clone.domain.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movieId;

    @Column(nullable = false)
    private String title;

    // Set by an admin, not derived from release date or schedules
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovieStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Genre genre;

    @Column(nullable = false)
    private Integer runningTime;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private String posterUrl;

    @Builder
    public Movie(String title, MovieStatus status, Genre genre, Integer runningTime, LocalDate releaseDate,
                 String posterUrl) {
        this.title = title;
        this.status = status;
        this.genre = genre;
        this.runningTime = runningTime;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
    }
}
