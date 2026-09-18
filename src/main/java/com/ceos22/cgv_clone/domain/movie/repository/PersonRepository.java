package com.ceos22.cgv_clone.domain.movie.repository;

import com.ceos22.cgv_clone.domain.movie.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
