package com.ceos22.cgv_clone.domain.movie.dto.response;

import com.ceos22.cgv_clone.domain.movie.entity.*;

import java.time.LocalDate;
import java.util.List;

public record MovieDetailResponse(
        Long movieId,
        String title,
        Genre genre,
        MovieStatus status,
        Integer runningTime,
        LocalDate releaseDate,
        String posterUrl,
        List<CreditInfo> directors,
        List<CreditInfo> actors
) {
    public record CreditInfo(Long personId, String name, String profileImageUrl, String characterName) {
        public static CreditInfo from(MovieCredit credit) {
            Person person = credit.getPerson();
            return new CreditInfo(person.getPersonId(), person.getName(), person.getProfileImageUrl(),
                    credit.getCharacterName());
        }
    }

    // credits must already be ordered by displayOrder
    public static MovieDetailResponse from(Movie movie, List<MovieCredit> credits) {
        return new MovieDetailResponse(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getGenre(),
                movie.getStatus(),
                movie.getRunningTime(),
                movie.getReleaseDate(),
                movie.getPosterUrl(),
                creditsOf(credits, CreditRole.DIRECTOR),
                creditsOf(credits, CreditRole.ACTOR)
        );
    }

    private static List<CreditInfo> creditsOf(List<MovieCredit> credits, CreditRole role) {
        return credits.stream()
                .filter(credit -> credit.getRole() == role)
                .map(CreditInfo::from)
                .toList();
    }
}
