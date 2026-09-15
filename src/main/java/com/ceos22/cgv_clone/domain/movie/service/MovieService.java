package com.ceos22.cgv_clone.domain.movie.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.movie.dto.request.MovieCreateRequest;
import com.ceos22.cgv_clone.domain.movie.dto.request.MovieCreateRequest.CreditRequest;
import com.ceos22.cgv_clone.domain.movie.dto.response.MovieDetailResponse;
import com.ceos22.cgv_clone.domain.movie.dto.response.MovieSimpleResponse;
import com.ceos22.cgv_clone.domain.movie.entity.Movie;
import com.ceos22.cgv_clone.domain.movie.entity.MovieCredit;
import com.ceos22.cgv_clone.domain.movie.entity.MovieStatus;
import com.ceos22.cgv_clone.domain.movie.entity.Person;
import com.ceos22.cgv_clone.domain.movie.repository.MovieCreditRepository;
import com.ceos22.cgv_clone.domain.movie.repository.MovieRepository;
import com.ceos22.cgv_clone.domain.movie.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final PersonRepository personRepository;

    public List<MovieSimpleResponse> findAllNowPlayingMovies() {
        List<Movie> movies = movieRepository.findByStatus(MovieStatus.NOW_PLAYING);

        return movies.stream()
                .map(MovieSimpleResponse::from)
                .collect(Collectors.toList());
    }

    public MovieDetailResponse findMovieById(Long movieId) {
        Movie movie = movieRepository.findById(movieId).
                orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));
        List<MovieCredit> credits = movieCreditRepository.findAllWithPersonByMovieId(movieId);

        return MovieDetailResponse.from(movie, credits);
    }

    @Transactional
    public MovieDetailResponse createMovie(MovieCreateRequest request) {
        validateNoDuplicateCredit(request.credits());
        Map<Long, Person> people = findPeopleById(request.credits());

        Movie movie = Movie.builder()
                .title(request.title())
                .genre(request.genre())
                .status(request.status())
                .runningTime(request.runningTime())
                .posterUrl(request.posterUrl())
                .build();
        Movie savedMovie = movieRepository.save(movie);

        List<MovieCredit> credits = new ArrayList<>();
        for (int i = 0; i < request.credits().size(); i++) {
            CreditRequest credit = request.credits().get(i);
            credits.add(MovieCredit.builder()
                    .movie(savedMovie)
                    .person(people.get(credit.personId()))
                    .role(credit.role())
                    .characterName(credit.characterName())
                    .displayOrder(i)
                    .build());
        }
        movieCreditRepository.saveAll(credits);

        return MovieDetailResponse.from(savedMovie, credits);
    }

    // =================================================================================================================

    private void validateNoDuplicateCredit(List<CreditRequest> credits) {
        long distinct = credits.stream()
                .map(credit -> credit.personId() + ":" + credit.role())
                .distinct()
                .count();
        if (distinct != credits.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Map<Long, Person> findPeopleById(List<CreditRequest> credits) {
        List<Long> personIds = credits.stream().map(CreditRequest::personId).distinct().toList();
        List<Person> people = personRepository.findAllById(personIds);
        if (people.size() != personIds.size()) {
            throw new CustomException(ErrorCode.PERSON_NOT_FOUND);
        }
        return people.stream().collect(Collectors.toMap(Person::getPersonId, Function.identity()));
    }
}
