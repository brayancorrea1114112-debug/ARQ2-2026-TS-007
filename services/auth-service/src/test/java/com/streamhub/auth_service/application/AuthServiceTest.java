package com.streamhub.auth_service.application;

import com.streamhub.auth_service.application.FakePorts.FakeTokens;
import com.streamhub.auth_service.application.FakePorts.InMemoryAccounts;
import com.streamhub.auth_service.application.FakePorts.PrefixHasher;
import com.streamhub.auth_service.application.ports.in.LoginUseCase.LoginResult;
import com.streamhub.auth_service.application.usecases.AuthService;
import com.streamhub.auth_service.domain.exception.InvalidCredentialsException;
import com.streamhub.auth_service.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final PrefixHasher hasher = new PrefixHasher();
    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private AuthService service;
    private Account ana;

    @BeforeEach
    void setUp() {
        ana = accounts.save(new Account(UUID.randomUUID(), "ana@example.com", hasher.hash("S3cret-pass"), Instant.now()));
        service = new AuthService(accounts, hasher, new FakeTokens());
    }

    @Test
    void validCredentialsReturnAnAccountToken() {
        LoginResult result = service.login("ana@example.com", "S3cret-pass");

        assertEquals(ana, result.account());
        assertEquals(3600, result.expiresIn());
        assertTrue(result.accessToken().startsWith("account:"));
    }

    @Test
    void emailIsNotCaseSensitiveAndIgnoresSurroundingSpaces() {
        LoginResult result = service.login("  ANA@Example.com ", "S3cret-pass");

        assertEquals(ana.id(), result.account().id());
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(InvalidCredentialsException.class, () -> service.login("ana@example.com", "wrong"));
    }

    @Test
    void unknownEmailGivesTheSameErrorAsAWrongPassword() {
        InvalidCredentialsException error =
                assertThrows(InvalidCredentialsException.class, () -> service.login("nobody@example.com", "S3cret-pass"));

        assertEquals("INVALID_CREDENTIALS", error.getCode());
    }
}
