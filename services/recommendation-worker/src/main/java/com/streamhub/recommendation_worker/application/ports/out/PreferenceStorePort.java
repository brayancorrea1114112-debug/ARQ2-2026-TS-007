package com.streamhub.recommendation_worker.application.ports.out;

import java.time.Instant;
import java.util.Map;

public interface PreferenceStorePort {

    void savePreference(String profileId, String contentId, double weight, Instant updatedAt);

    Map<String, Double> findByProfile(String profileId);
}
