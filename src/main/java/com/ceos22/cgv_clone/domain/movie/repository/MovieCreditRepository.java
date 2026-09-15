package com.ceos22.cgv_clone.domain.movie.repository;

import com.ceos22.cgv_clone.domain.movie.entity.MovieCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieCreditRepository extends JpaRepository<MovieCredit, Long> {

    @Query("SELECT c FROM MovieCredit c JOIN FETCH c.person " +
            "WHERE c.movie.movieId = :movieId " +
            "ORDER BY c.displayOrder ASC")
    List<MovieCredit> findAllWithPersonByMovieId(@Param("movieId") Long movieId);
}
