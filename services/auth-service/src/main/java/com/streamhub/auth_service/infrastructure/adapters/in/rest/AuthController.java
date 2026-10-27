package com.streamhub.auth_service.infrastructure.adapters.in.rest;

import com.streamhub.auth_service.application.ports.in.LoginUseCase;
import com.streamhub.auth_service.application.ports.in.LoginUseCase.LoginResult;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.AuthDtos.LoginRequest;
import com.streamhub.auth_service.infrastructure.adapters.in.rest.dto.AuthDtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.login(request.email(), request.password());
        return LoginResponse.of(result.accessToken(), result.expiresIn(), result.account());
    }
}
