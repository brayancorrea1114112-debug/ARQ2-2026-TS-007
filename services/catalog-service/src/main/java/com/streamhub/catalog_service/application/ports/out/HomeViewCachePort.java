package com.streamhub.catalog_service.application.ports.out;

import com.streamhub.catalog_service.domain.model.HomeView;

import java.time.Duration;
import java.util.Optional;

/** Cache-aside storage for the Home and Continue Watching views. */
public interface HomeViewCachePort {

    /**
     * @return the view of the profile, or empty when it is not stored
     * @throws com.streamhub.catalog_service.domain.exception.ServiceUnavailableException when the cache cannot be reached
     */
    Optional<HomeView> findByProfileId(String profileId);

    /** Stores the Home and Continue Watching entries with their respective TTLs. */
    void save(HomeView view, Duration homeTtl, Duration continueWatchingTtl);
}
