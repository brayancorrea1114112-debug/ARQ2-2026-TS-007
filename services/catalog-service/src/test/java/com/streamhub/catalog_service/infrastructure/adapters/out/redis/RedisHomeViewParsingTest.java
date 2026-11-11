package com.streamhub.catalog_service.infrastructure.adapters.out.redis;

import com.streamhub.catalog_service.domain.model.CategoryRef;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Reads legacy combined Home values while existing entries expire during the cache migration. */
class RedisHomeViewParsingTest {

    private static final String LEGACY_HOME_JSON = """
            {"profile_id":"prof_9921","generated_at":"2026-10-06T19:58:00.123456Z","categories":[],
             "continue_watching":[{"content_id":"mov_8832","episode_id":"ep_102","progress_seconds":1420},
                                  {"content_id":"mov_5","episode_id":null,"progress_seconds":30}],
             "recommended":["mov_77","mov_91"]}""";

    @Test
    void theViewWrittenByTheWorkerIsParsed() {
        HomeView view = RedisHomeViewAdapter.parse("prof_9921", LEGACY_HOME_JSON);

        assertEquals("prof_9921", view.profileId());
        assertEquals(Instant.parse("2026-10-06T19:58:00.123456Z"), view.generatedAt());
        assertEquals(List.of(), view.categories());
        assertEquals(List.of("mov_77", "mov_91"), view.recommended());
        assertEquals(new ContinueWatchingItem("mov_8832", "ep_102", 1420), view.continueWatching().get(0));
    }

    @Test
    void aNullEpisodeIdIsAccepted() {
        HomeView view = RedisHomeViewAdapter.parse("prof_9921", LEGACY_HOME_JSON);

        assertNull(view.continueWatching().get(1).episodeId());
    }

    @Test
    void unknownFieldsAreIgnoredAndCategoriesAreRead() {
        String json = """
                {"profile_id":"prof_9921","generated_at":"2026-10-06T19:58:00Z","something_new":{"a":1},
                 "categories":[{"name":"Trending","title_ids":["mov_1","mov_2"],"extra":true}],
                 "continue_watching":[],"recommended":[]}""";

        HomeView view = RedisHomeViewAdapter.parse("prof_9921", json);

        assertEquals(List.of(new CategoryRef("Trending", List.of("mov_1", "mov_2"))), view.categories());
    }

    @Test
    void missingSectionsAreTreatedAsEmpty() {
        HomeView view = RedisHomeViewAdapter.parse("prof_9921", "{\"generated_at\":\"2026-10-06T19:58:00Z\"}");

        assertEquals(List.of(), view.categories());
        assertEquals(List.of(), view.continueWatching());
        assertEquals(List.of(), view.recommended());
    }

    @Test
    void theProfileIdIsTheOneOfTheKeyNotTheOneInsideTheDocument() {
        assertEquals("prof_1", RedisHomeViewAdapter.parse("prof_1", LEGACY_HOME_JSON).profileId());
    }

    @Test
    void brokenDocumentsFailToParse() {
        assertThrows(RuntimeException.class, () -> RedisHomeViewAdapter.parse("prof_1", "not json"));
        assertThrows(RuntimeException.class, () -> RedisHomeViewAdapter.parse("prof_1", "{}"));
        assertThrows(RuntimeException.class,
                () -> RedisHomeViewAdapter.parse("prof_1", "{\"generated_at\":\"yesterday\"}"));
    }
}
