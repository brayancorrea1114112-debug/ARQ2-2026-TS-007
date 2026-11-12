package com.streamhub.catalog_service.application.usecases;

import com.streamhub.catalog_service.application.ports.in.ListTitleGenresUseCase;
import com.streamhub.catalog_service.application.ports.out.TitleGenresPort;
import com.streamhub.catalog_service.domain.model.TitleGenres;

import java.util.List;

/** The catalog owns the genres; other services ask for them instead of reading its database (ADR 009). */
public class TitleGenresService implements ListTitleGenresUseCase {

    private final TitleGenresPort genres;

    public TitleGenresService(TitleGenresPort genres) {
        this.genres = genres;
    }

    @Override
    public List<TitleGenres> listTitleGenres() {
        return genres.findAll();
    }
}
