package com.streamhub.catalog_service.domain.model;

/** Whether the Home came from an existing cache entry or was rebuilt from the source stores. */
public enum HomeSource {
    CACHE,
    FALLBACK
}
