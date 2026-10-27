package com.streamhub.auth_service.application.ports.in;

import com.streamhub.auth_service.domain.model.Account;

public interface LoginUseCase {

    LoginResult login(String email, String password);

    record LoginResult(String accessToken, long expiresIn, Account account) {
    }
}
