package com.streamhub.catalog_service.application.ports.in;

import com.streamhub.catalog_service.domain.model.TitleGenres;

import java.util.List;

/** Use case behind the internal GET /internal/v1/titles/genres (ADR 009). */
public interface ListTitleGenresUseCase {

    List<TitleGenres> listTitleGenres();
}
