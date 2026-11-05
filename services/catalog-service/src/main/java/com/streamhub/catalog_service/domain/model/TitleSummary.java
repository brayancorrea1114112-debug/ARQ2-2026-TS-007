package com.streamhub.catalog_service.domain.model;

/** What the Home screen shows of a title: its id, its name and its poster. The poster may be missing. */
public record TitleSummary(String contentId, String name, String poster) {

    public TitleSummary {
        Require.notBlank(contentId, "contentId");
        Require.notBlank(name, "title name");
    }
}
