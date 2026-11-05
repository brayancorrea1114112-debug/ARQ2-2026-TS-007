package com.streamhub.catalog_service.infrastructure.fixtures;

import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Sample catalog kept in memory, for the tests that do not need a real MongoDB. */
public class InMemoryTitleSummaryAdapter implements TitleSummaryPort {

    /** In order of popularity, so the first ones are the "trending" titles. */
    private final Map<String, TitleSummary> titles = new LinkedHashMap<>();

    public InMemoryTitleSummaryAdapter() {
        add("mov_1", "Night Harbor");
        add("mov_2", "The Last Orchard");
        add("mov_8832", "Example Series");
        add("mov_77", "Paper Cities");
        add("mov_91", "Signal Fire");
        add("mov_3", "Winter Run");
        add("mov_4", "Glass Garden");
        add("mov_5", "Echoes of Salt");
        add("mov_6", "Quiet Machines");
        add("mov_7", "Blue Hour");
    }

    private void add(String contentId, String name) {
        titles.put(contentId, new TitleSummary(contentId, name, "https://cdn.example.com/p/" + contentId + ".jpg"));
    }

    @Override
    public List<TitleSummary> findByIds(List<String> titleIds) {
        return titleIds.stream().map(titles::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<TitleSummary> findTrending(int limit) {
        return titles.values().stream().limit(limit).toList();
    }
}
