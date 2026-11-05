package com.streamhub.catalog_service.domain.model;

/**
 * A title the profile is in the middle of watching (spec.md 2.4). The episode id is only present
 * for series.
 */
public record ContinueWatchingItem(String contentId, String episodeId, int progressSeconds) {

    public ContinueWatchingItem {
        Require.notBlank(contentId, "contentId");
        Require.zeroOrMore(progressSeconds, "progressSeconds");
    }
}
