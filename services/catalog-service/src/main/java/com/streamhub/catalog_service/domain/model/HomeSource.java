package com.streamhub.catalog_service.domain.model;

/** Where a Home screen came from (spec.md 3.4): the Redis view, or the generic one built from MongoDB. */
public enum HomeSource {
    CACHE,
    FALLBACK
}
