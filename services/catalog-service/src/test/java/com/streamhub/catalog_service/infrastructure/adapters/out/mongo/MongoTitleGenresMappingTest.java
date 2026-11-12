package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.domain.model.TitleGenres;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoTitleGenresMappingTest {

    @Test
    void theGenresOfADocumentOfTheSpecAreMapped() {
        Document document = new Document("_id", "mov_8832").append("genres", List.of("Drama", "Thriller"));

        assertEquals(Optional.of(new TitleGenres("mov_8832", List.of("Drama", "Thriller"))),
                MongoTitleGenresAdapter.toTitleGenres(document));
    }

    @Test
    void aTitleWithoutGenresIsListedWithNone() {
        assertEquals(Optional.of(new TitleGenres("mov_1", List.of())),
                MongoTitleGenresAdapter.toTitleGenres(new Document("_id", "mov_1")));
    }

    @Test
    void aDocumentWithUnreadableGenresIsLeftOut() {
        Document document = new Document("_id", "mov_1").append("genres", "Drama");

        assertEquals(Optional.empty(), MongoTitleGenresAdapter.toTitleGenres(document));
    }
}
