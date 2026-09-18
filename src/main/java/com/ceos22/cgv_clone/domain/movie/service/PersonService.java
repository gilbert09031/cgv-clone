package com.ceos22.cgv_clone.domain.movie.service;

import com.ceos22.cgv_clone.domain.movie.dto.request.PersonCreateRequest;
import com.ceos22.cgv_clone.domain.movie.dto.response.PersonResponse;
import com.ceos22.cgv_clone.domain.movie.entity.Person;
import com.ceos22.cgv_clone.domain.movie.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public PersonResponse createPerson(PersonCreateRequest request) {
        Person person = Person.builder()
                .name(request.name())
                .birthDate(request.birthDate())
                .profileImageUrl(request.profileImageUrl())
                .build();

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }
}
