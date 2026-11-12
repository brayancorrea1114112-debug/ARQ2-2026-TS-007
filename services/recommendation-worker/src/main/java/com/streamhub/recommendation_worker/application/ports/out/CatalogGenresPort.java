package com.streamhub.recommendation_worker.application.ports.out;

import java.util.List;
import java.util.Map;

/**
 * The genres of the catalog titles. They belong to the Catalog & Home Service, so the worker asks for them
 * instead of reading its database (ADR 009).
 */
public interface CatalogGenresPort {

    /**
     * @return content_id to its genres, for every title of the catalog
     * @throws CatalogUnavailableException when the genres cannot be obtained
     */
    Map<String, List<String>> genresByTitle();

    class CatalogUnavailableException extends RuntimeException {

        public CatalogUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
