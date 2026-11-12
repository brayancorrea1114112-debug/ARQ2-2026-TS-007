package com.streamhub.recommendation_worker.infrastructure.adapters.out.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls GET /internal/v1/titles/genres of the Catalog & Home Service inside the Docker network (spec.md 3.7).
 * The RestClient comes from Spring Boot, so the call is a span of the trace of the event being processed.
 */
public class HttpCatalogGenresClient implements CatalogGenresPort {

    static final String PATH = "/internal/v1/titles/genres";

    private final RestClient restClient;

    public HttpCatalogGenresClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Map<String, List<String>> genresByTitle() {
        GenresResponse response;
        try {
            response = restClient.get().uri(PATH).retrieve().body(GenresResponse.class);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("The catalog did not answer the genres", e);
        }
        if (response == null || response.titles() == null) {
            throw new CatalogUnavailableException("The catalog answered without genres", null);
        }
        Map<String, List<String>> genres = new HashMap<>();
        for (TitleDto title : response.titles()) {
            if (title.contentId() != null) {
                genres.put(title.contentId(), title.genres() == null ? List.of() : List.copyOf(title.genres()));
            }
        }
        return Map.copyOf(genres);
    }

    record GenresResponse(List<TitleDto> titles) {
    }

    record TitleDto(@JsonProperty("content_id") String contentId, List<String> genres) {
    }
}
