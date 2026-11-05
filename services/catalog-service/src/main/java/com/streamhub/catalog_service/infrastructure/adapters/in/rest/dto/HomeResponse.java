package com.streamhub.catalog_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeCategory;
import com.streamhub.catalog_service.domain.model.HomeScreen;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** JSON of GET /api/v1/home (spec.md 3.4). The domain model is not exposed directly: the wire format lives here. */
public record HomeResponse(@JsonProperty("profile_id") String profileId,
                           @JsonProperty("generated_at") Instant generatedAt,
                           String source,
                           List<CategoryDto> categories,
                           @JsonProperty("continue_watching") List<ContinueWatchingDto> continueWatching,
                           List<TitleDto> recommended) {

    public record CategoryDto(String name, List<TitleDto> titles) {
    }

    public record TitleDto(@JsonProperty("content_id") String contentId, String name, String poster) {
    }

    public record ContinueWatchingDto(@JsonProperty("content_id") String contentId,
                                      @JsonProperty("episode_id") @JsonInclude(JsonInclude.Include.NON_NULL) String episodeId,
                                      @JsonProperty("progress_seconds") int progressSeconds) {
    }

    public static HomeResponse from(HomeScreen screen) {
        return new HomeResponse(screen.profileId(), screen.generatedAt(),
                screen.source().name().toLowerCase(Locale.ROOT),
                screen.categories().stream().map(HomeResponse::category).toList(),
                screen.continueWatching().stream().map(HomeResponse::continueWatching).toList(),
                screen.recommended().stream().map(HomeResponse::title).toList());
    }

    private static CategoryDto category(HomeCategory category) {
        return new CategoryDto(category.name(), category.titles().stream().map(HomeResponse::title).toList());
    }

    private static TitleDto title(TitleSummary title) {
        return new TitleDto(title.contentId(), title.name(), title.poster());
    }

    private static ContinueWatchingDto continueWatching(ContinueWatchingItem item) {
        return new ContinueWatchingDto(item.contentId(), item.episodeId(), item.progressSeconds());
    }
}
