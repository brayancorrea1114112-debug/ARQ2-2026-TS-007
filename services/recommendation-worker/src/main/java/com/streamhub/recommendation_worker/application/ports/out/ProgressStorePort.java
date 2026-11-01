package com.streamhub.recommendation_worker.application.ports.out;

import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;

import java.util.List;
import java.util.Optional;

public interface ProgressStorePort {

    void saveProgress(ProgressSnapshot snapshot);

    Optional<ProgressSnapshot> find(String profileId, String contentId);

    List<ProgressSnapshot> findByProfile(String profileId);
}
