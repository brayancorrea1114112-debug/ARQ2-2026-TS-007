package com.streamhub.auth_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.auth_service.domain.model.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** Request and response bodies of spec.md 3.1. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AccountSummary(@JsonProperty("account_id") UUID accountId, String email) {

        public static AccountSummary from(Account account) {
            return new AccountSummary(account.id(), account.email());
        }
    }

    public record LoginResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            AccountSummary account) {

        public static LoginResponse of(String accessToken, long expiresIn, Account account) {
            return new LoginResponse(accessToken, "Bearer", expiresIn, AccountSummary.from(account));
        }
    }
}
