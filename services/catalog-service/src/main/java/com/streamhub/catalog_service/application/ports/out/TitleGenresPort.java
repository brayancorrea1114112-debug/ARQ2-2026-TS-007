package com.streamhub.catalog_service.application.ports.out;

import com.streamhub.catalog_service.domain.model.TitleGenres;

import java.util.List;

/**
 * Where the genres of the catalog titles are read from.
 *
 * @throws com.streamhub.catalog_service.domain.exception.ServiceUnavailableException when the catalog cannot be reached
 */
public interface TitleGenresPort {

    List<TitleGenres> findAll();
}
