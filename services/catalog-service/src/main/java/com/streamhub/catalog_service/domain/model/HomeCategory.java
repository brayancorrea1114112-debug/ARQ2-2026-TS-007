package com.streamhub.catalog_service.domain.model;

import java.util.List;

/** A Home category as it is answered to the client: its name and the summaries of its titles. */
public record HomeCategory(String name, List<TitleSummary> titles) {

    public HomeCategory {
        Require.notBlank(name, "category name");
        titles = Require.listOrEmpty(titles);
    }
}
