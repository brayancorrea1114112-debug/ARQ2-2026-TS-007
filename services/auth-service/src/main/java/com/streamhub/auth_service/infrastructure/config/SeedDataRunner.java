package com.streamhub.auth_service.infrastructure.config;

import com.streamhub.auth_service.application.ports.out.AccountRepositoryPort;
import com.streamhub.auth_service.application.ports.out.PasswordHasherPort;
import com.streamhub.auth_service.application.ports.out.ProfileRepositoryPort;
import com.streamhub.auth_service.domain.model.Account;
import com.streamhub.auth_service.domain.model.Profile;
import com.streamhub.auth_service.domain.model.ProfileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Development data. spec.md has no sign-up endpoint, so without this there would be no account to log in with.
 * It only runs when seed.enabled=true and never overwrites an existing account.
 */
@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final AccountRepositoryPort accounts;
    private final ProfileRepositoryPort profiles;
    private final PasswordHasherPort passwordHasher;
    private final String password;

    public SeedDataRunner(AccountRepositoryPort accounts, ProfileRepositoryPort profiles,
                          PasswordHasherPort passwordHasher, @Value("${seed.password:}") String password) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.passwordHasher = passwordHasher;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password.isBlank()) {
            log.warn("seed.enabled is true but SEED_PASSWORD is empty: no data was created");
            return;
        }
        createAccount("ana@example.com",
                new String[][]{{"prof_9921", "Ana", "ADULT"}, {"prof_9922", "Sam", "KIDS"}});
        createAccount("luis@example.com",
                new String[][]{{"prof_9931", "Luis", "ADULT"}});
    }

    private void createAccount(String email, String[][] accountProfiles) {
        if (accounts.findByEmail(email).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        Account account = accounts.save(new Account(UUID.randomUUID(), email, passwordHasher.hash(password), now));
        for (String[] data : accountProfiles) {
            profiles.save(new Profile(data[0], account.id(), data[1], ProfileType.valueOf(data[2]),
                    "https://cdn.example.com/a/" + data[0] + ".png", now));
        }
        log.info("Seeded account {} with {} profile(s)", email, accountProfiles.length);
    }
}
