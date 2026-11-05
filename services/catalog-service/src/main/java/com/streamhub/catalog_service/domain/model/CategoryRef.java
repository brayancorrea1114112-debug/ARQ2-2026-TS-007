package com.streamhub.catalog_service.domain.model;

import java.util.List;

/** A Home category as it is stored: its name and only the ids of its titles (spec.md 2.4). */
public record CategoryRef(String name, List<String> titleIds) {

    public CategoryRef {
        Require.notBlank(name, "category name");
        titleIds = Require.listOrEmpty(titleIds);
    }
}
