package com.streamhub.catalog_service.application.ports.out;

import com.streamhub.catalog_service.domain.model.HomeView;

import java.util.Optional;

/** Where the precomputed Home views are read from (Redis, key home:{profile_id}, spec.md 2.4). */
public interface HomeViewCachePort {

    /**
     * @return the view of the profile, or empty when it is not stored
     * @throws com.streamhub.catalog_service.domain.exception.ServiceUnavailableException when the cache cannot be reached
     */
    Optional<HomeView> findByProfileId(String profileId);
}
