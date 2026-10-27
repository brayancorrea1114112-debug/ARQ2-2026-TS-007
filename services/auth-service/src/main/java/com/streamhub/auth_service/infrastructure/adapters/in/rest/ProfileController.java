package com.streamhub.auth_service.infrastructure.adapters.in.rest;

import com.streamhub.auth_service.application.ports.in.ProfileUseCase;
import com.streamhub.auth_service.application.ports.in.ProfileUseCase.ProfileSelection;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ProfileDtos.ProfileSummary;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ProfileDtos.ProfilesResponse;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ProfileDtos.SelectProfileRequest;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.ProfileDtos.SelectProfileResponse;
import com.streamhub.auth_service.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileUseCase profileUseCase;

    public ProfileController(ProfileUseCase profileUseCase) {
        this.profileUseCase = profileUseCase;
    }

    @GetMapping
    public ProfilesResponse list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProfilesResponse.from(profileUseCase.listProfiles(user.accountId()));
    }

    @PostMapping("/select")
    public SelectProfileResponse select(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody SelectProfileRequest request) {
        ProfileSelection selection = profileUseCase.selectProfile(user.accountId(), request.profileId());
        return new SelectProfileResponse(selection.accessToken(), "Bearer", selection.expiresIn(),
                ProfileSummary.withoutAvatar(selection.profile()));
    }
}
