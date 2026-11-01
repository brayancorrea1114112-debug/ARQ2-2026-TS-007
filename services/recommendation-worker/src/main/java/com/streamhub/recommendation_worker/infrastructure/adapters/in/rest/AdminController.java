package com.streamhub.recommendation_worker.infrastructure.adapters.in.rest;

import com.streamhub.recommendation_worker.application.usecases.RedisProjectionRebuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Internal operational endpoint. It is intentionally not routed through Kong.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RedisProjectionRebuilder rebuilder;
    private final String adminToken;

    public AdminController(RedisProjectionRebuilder rebuilder,
                           @Value("${admin.token}") String adminToken) {
        this.rebuilder = rebuilder;
        this.adminToken = adminToken;
    }

    @PostMapping("/rebuild")
    public Map<String, Object> rebuildAll(
            @RequestHeader(name = "X-Admin-Token", required = false) String token) {
        authorize(token);
        return Map.of("rebuilt_profiles", rebuilder.rebuildAll());
    }

    @PostMapping("/rebuild/{profileId}")
    public Map<String, Object> rebuildProfile(
            @PathVariable String profileId,
            @RequestHeader(name = "X-Admin-Token", required = false) String token) {
        authorize(token);
        rebuilder.rebuildProfile(profileId);
        return Map.of("rebuilt_profiles", 1, "profile_id", profileId);
    }

    private void authorize(String token) {
        if (token == null || !adminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token");
        }
    }
}
