package com.ceos22.cgv_clone.domain.movie.controller;

import com.ceos22.cgv_clone.common.error.SuccessCode;
import com.ceos22.cgv_clone.common.response.ApiResponse;
import com.ceos22.cgv_clone.domain.movie.dto.request.PersonCreateRequest;
import com.ceos22.cgv_clone.domain.movie.dto.response.PersonResponse;
import com.ceos22.cgv_clone.domain.movie.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/people")
@RequiredArgsConstructor
public class AdminPersonController {

    private final PersonService personService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PersonResponse>> createPerson(
            @Valid @RequestBody PersonCreateRequest request
    ) {
        PersonResponse createdPerson = personService.createPerson(request);
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, createdPerson);
    }
}
