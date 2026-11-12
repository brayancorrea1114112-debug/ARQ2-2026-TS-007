package com.streamhub.catalog_service.infrastructure.adapters.in.rest;

import com.streamhub.catalog_service.application.ports.in.ListTitleGenresUseCase;
import com.streamhub.catalog_service.infrastructure.adapters.in.rest.dto.TitleGenresResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API (ADR 009). Kong does not route /internal, so it is only reachable inside the Docker
 * network. It needs no token: it only exposes public catalog data and no user data.
 */
@RestController
@RequestMapping("/internal/v1/titles")
public class InternalTitleController {

    private final ListTitleGenresUseCase listTitleGenres;

    public InternalTitleController(ListTitleGenresUseCase listTitleGenres) {
        this.listTitleGenres = listTitleGenres;
    }

    @GetMapping(value = "/genres", produces = "application/json;charset=UTF-8")
    public TitleGenresResponse genres() {
        return TitleGenresResponse.from(listTitleGenres.listTitleGenres());
    }
}
