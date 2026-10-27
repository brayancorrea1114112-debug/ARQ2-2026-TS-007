package com.streamhub.auth_service.infrastructure.security;

import com.streamhub.auth_service.infrastructure.adapters.out.security.JwtTokenAdapter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Kong already validates the JWT, but the service checks it again so it never trusts the network.
 * Without a valid token the request is left unauthenticated and the entry point answers 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenAdapter tokens;

    public JwtAuthenticationFilter(JwtTokenAdapter tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokens.verify(header.substring(BEARER_PREFIX.length()).trim()).ifPresent(claims -> {
                AuthenticatedUser user = new AuthenticatedUser(claims.accountId(), claims.profileId());
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            });
        }
        chain.doFilter(request, response);
    }
}
