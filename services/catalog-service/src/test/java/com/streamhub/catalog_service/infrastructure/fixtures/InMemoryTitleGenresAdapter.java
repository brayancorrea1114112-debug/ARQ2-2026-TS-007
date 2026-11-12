package com.streamhub.catalog_service.infrastructure.fixtures;

import com.streamhub.catalog_service.application.ports.out.TitleGenresPort;
import com.streamhub.catalog_service.domain.model.TitleGenres;

import java.util.List;

/** Sample genres kept in memory, for the tests that do not need a real MongoDB. */
public class InMemoryTitleGenresAdapter implements TitleGenresPort {

    @Override
    public List<TitleGenres> findAll() {
        return List.of(
                new TitleGenres("mov_1", List.of("Drama", "Thriller")),
                new TitleGenres("mov_2", List.of("Comedy")),
                new TitleGenres("mov_3", List.of()));
    }
}
