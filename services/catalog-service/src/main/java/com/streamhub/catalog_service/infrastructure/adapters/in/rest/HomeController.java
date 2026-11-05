package com.streamhub.catalog_service.infrastructure.adapters.in.rest;

import com.streamhub.catalog_service.application.ports.in.GetHomeUseCase;
import com.streamhub.catalog_service.infrastructure.adapters.in.rest.dto.HomeResponse;
import com.streamhub.catalog_service.infrastructure.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final GetHomeUseCase getHome;

    public HomeController(GetHomeUseCase getHome) {
        this.getHome = getHome;
    }

    /** An account token has no profile: the use case refuses it with PROFILE_NOT_SELECTED. */
    /** spec.md 1.3 asks for "application/json; charset=utf-8", which Spring does not add by itself. */
    @GetMapping(produces = "application/json;charset=UTF-8")
    public HomeResponse home(@AuthenticationPrincipal AuthenticatedUser user) {
        return HomeResponse.from(getHome.getHome(user.profileId()));
    }
}
