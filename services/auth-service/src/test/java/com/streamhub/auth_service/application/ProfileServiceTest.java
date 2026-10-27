package com.streamhub.auth_service.application;

import com.streamhub.auth_service.application.FakePorts.FakeTokens;
import com.streamhub.auth_service.application.FakePorts.InMemoryProfiles;
import com.streamhub.auth_service.application.ports.in.ProfileUseCase.ProfileSelection;
import com.streamhub.auth_service.application.usecases.ProfileService;
import com.streamhub.auth_service.domain.exception.ProfileForbiddenException;
import com.streamhub.auth_service.domain.exception.ProfileNotFoundException;
import com.streamhub.auth_service.domain.model.Profile;
import com.streamhub.auth_service.domain.model.ProfileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileServiceTest {

    private final UUID anaId = UUID.randomUUID();
    private final UUID luisId = UUID.randomUUID();
    private ProfileService service;

    @BeforeEach
    void setUp() {
        InMemoryProfiles profiles = new InMemoryProfiles();
        profiles.save(new Profile("prof_9921", anaId, "Ana", ProfileType.ADULT, null, Instant.now()));
        profiles.save(new Profile("prof_9922", anaId, "Sam", ProfileType.KIDS, null, Instant.now()));
        profiles.save(new Profile("prof_9931", luisId, "Luis", ProfileType.ADULT, null, Instant.now()));
        service = new ProfileService(profiles, new FakeTokens());
    }

    @Test
    void listsOnlyTheProfilesOfTheAccount() {
        List<Profile> result = service.listProfiles(anaId);

        assertEquals(2, result.size());
        assertEquals(true, result.stream().allMatch(profile -> profile.belongsTo(anaId)));
    }

    @Test
    void selectingAnOwnProfileReturnsAProfileToken() {
        ProfileSelection selection = service.selectProfile(anaId, "prof_9921");

        assertEquals("prof_9921", selection.profile().id());
        assertEquals("profile:" + anaId + ":prof_9921", selection.accessToken());
    }

    @Test
    void selectingAProfileOfAnotherAccountIsForbidden() {
        assertThrows(ProfileForbiddenException.class, () -> service.selectProfile(anaId, "prof_9931"));
    }

    @Test
    void selectingAProfileThatDoesNotExistIsNotFound() {
        assertThrows(ProfileNotFoundException.class, () -> service.selectProfile(anaId, "prof_0000"));
    }
}
