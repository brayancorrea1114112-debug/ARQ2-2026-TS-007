package com.streamhub.auth_service.application.usecases;

import com.streamhub.auth_service.application.ports.in.LoginUseCase;
import com.streamhub.auth_service.application.ports.out.AccountRepositoryPort;
import com.streamhub.auth_service.application.ports.out.PasswordHasherPort;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort.IssuedToken;
import com.streamhub.auth_service.domain.exception.InvalidCredentialsException;
import com.streamhub.auth_service.domain.model.Account;

import java.util.Locale;

public class AuthService implements LoginUseCase {

    private final AccountRepositoryPort accounts;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    public AuthService(AccountRepositoryPort accounts, PasswordHasherPort passwordHasher, TokenIssuerPort tokenIssuer) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public LoginResult login(String email, String password) {
        // The same error for an unknown email and for a wrong password, so the API does not reveal which emails exist.
        Account account = accounts.findByEmail(normalize(email))
                .filter(found -> passwordHasher.matches(password, found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        IssuedToken token = tokenIssuer.issueAccountToken(account.id());
        return new LoginResult(token.value(), token.expiresIn(), account);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
