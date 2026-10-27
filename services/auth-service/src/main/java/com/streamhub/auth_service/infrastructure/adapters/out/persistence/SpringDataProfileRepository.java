package com.streamhub.auth_service.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataProfileRepository extends JpaRepository<ProfileEntity, String> {

    List<ProfileEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);
}
