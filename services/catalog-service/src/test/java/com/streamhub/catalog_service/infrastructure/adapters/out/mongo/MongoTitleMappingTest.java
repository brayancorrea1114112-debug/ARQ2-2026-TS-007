package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.domain.model.TitleSummary;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoTitleMappingTest {

    @Test
    void aDocumentOfTheSpecIsMappedToASummary() {
        Document document = new Document("_id", "mov_8832").append("name", "Example Series")
                .append("images", new Document("poster", "https://cdn.example.com/p/mov_8832.jpg"));

        assertEquals(Optional.of(new TitleSummary("mov_8832", "Example Series", "https://cdn.example.com/p/mov_8832.jpg")),
                MongoTitleSummaryAdapter.toSummary(document));
    }

    @Test
    void aTitleWithoutImagesHasNoPoster() {
        Document document = new Document("_id", "mov_1").append("name", "One");

        assertEquals(Optional.of(new TitleSummary("mov_1", "One", null)), MongoTitleSummaryAdapter.toSummary(document));
    }

    @Test
    void aTitleWithoutNameIsLeftOut() {
        assertEquals(Optional.empty(), MongoTitleSummaryAdapter.toSummary(new Document("_id", "mov_1")));
    }
}
