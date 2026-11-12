package com.streamhub.catalog_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.catalog_service.domain.model.TitleGenres;

import java.util.List;

/** JSON of GET /internal/v1/titles/genres (spec.md 3.7). */
public record TitleGenresResponse(List<TitleDto> titles) {

    public record TitleDto(@JsonProperty("content_id") String contentId, List<String> genres) {
    }

    public static TitleGenresResponse from(List<TitleGenres> titles) {
        return new TitleGenresResponse(titles.stream()
                .map(title -> new TitleDto(title.contentId(), title.genres()))
                .toList());
    }
}
