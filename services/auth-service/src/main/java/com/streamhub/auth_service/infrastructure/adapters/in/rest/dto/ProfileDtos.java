package com.streamhub.auth_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.auth_service.domain.model.Profile;
import com.streamhub.auth_service.domain.model.ProfileType;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Request and response bodies of spec.md 3.2 and 3.3. */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record SelectProfileRequest(@NotBlank @JsonProperty("profile_id") String profileId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProfileSummary(
            @JsonProperty("profile_id") String profileId,
            String name,
            ProfileType type,
            @JsonProperty("avatar_url") String avatarUrl) {

        public static ProfileSummary from(Profile profile) {
            return new ProfileSummary(profile.id(), profile.name(), profile.type(), profile.avatarUrl());
        }

        /** The select response only carries id, name and type. */
        public static ProfileSummary withoutAvatar(Profile profile) {
            return new ProfileSummary(profile.id(), profile.name(), profile.type(), null);
        }
    }

    public record ProfilesResponse(List<ProfileSummary> profiles) {

        public static ProfilesResponse from(List<Profile> profiles) {
            return new ProfilesResponse(profiles.stream().map(ProfileSummary::from).toList());
        }
    }

    public record SelectProfileResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            ProfileSummary profile) {
    }
}
