package com.streamhub.recommendation_worker.application.ports.out;

import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;

import java.util.List;
import java.util.Optional;

public interface RatingStorePort {

    void saveRating(RatingSnapshot snapshot);

    Optional<RatingSnapshot> find(String profileId, String contentId);

    List<RatingSnapshot> findByProfile(String profileId);
}
