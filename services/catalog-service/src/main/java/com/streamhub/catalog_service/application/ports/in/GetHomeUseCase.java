package com.streamhub.catalog_service.application.ports.in;

import com.streamhub.catalog_service.domain.model.HomeScreen;

/** Use case behind GET /api/v1/home (spec.md 3.4). */
public interface GetHomeUseCase {

    /**
     * @param profileId the active profile taken from the token; blank or null means that no profile was selected
     */
    HomeScreen getHome(String profileId);
}
