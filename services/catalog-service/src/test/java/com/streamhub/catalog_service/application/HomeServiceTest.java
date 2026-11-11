package com.streamhub.catalog_service.application;

import com.streamhub.catalog_service.application.FakePorts.InMemoryHomeViews;
import com.streamhub.catalog_service.application.FakePorts.InMemoryInteractions;
import com.streamhub.catalog_service.application.FakePorts.InMemoryTitles;
import com.streamhub.catalog_service.application.usecases.HomeService;
import com.streamhub.catalog_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.CategoryRef;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeScreen;
import com.streamhub.catalog_service.domain.model.HomeSource;
import com.streamhub.catalog_service.domain.model.HomeView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.streamhub.catalog_service.application.FakePorts.summary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scenarios of spec.md, Feature 2: Home screen in milliseconds from cache. */
class HomeServiceTest {

    private static final String PROFILE = "prof_9921";
    private static final Instant NOW = Instant.parse("2026-10-06T20:00:00Z");
    private static final Instant GENERATED_AT = Instant.parse("2026-10-06T19:58:00Z");

    private InMemoryHomeViews homeViews;
    private InMemoryInteractions interactions;
    private InMemoryTitles titles;
    private HomeService service;

    @BeforeEach
    void setUp() {
        homeViews = new InMemoryHomeViews();
        interactions = new InMemoryInteractions();
        titles = new InMemoryTitles();
        service = new HomeService(homeViews, titles, interactions, Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(15), Duration.ofHours(24));
    }

    @Test
    void aStoredViewIsAnsweredWithNamesAndPostersAndMarkedAsCache() {
        titles.add("mov_1", "mov_2", "mov_77");
        ContinueWatchingItem watching = new ContinueWatchingItem("mov_8832", "ep_102", 1420);
        homeViews.store(new HomeView(PROFILE, GENERATED_AT,
                List.of(new CategoryRef("Trending", List.of("mov_1", "mov_2"))),
                List.of(watching), List.of("mov_77")));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(HomeSource.CACHE, screen.source());
        assertEquals(GENERATED_AT, screen.generatedAt());
        assertEquals(1, screen.categories().size());
        assertEquals("Trending", screen.categories().get(0).name());
        assertEquals(List.of(summary("mov_1"), summary("mov_2")), screen.categories().get(0).titles());
        assertEquals(List.of(summary("mov_77")), screen.recommended());
        assertEquals(List.of(watching), screen.continueWatching());
        assertEquals(0, titles.findTrendingCalls);
    }

    @Test
    void aMissingViewIsRebuiltFromTrendingAndInteractionsAndCached() {
        titles.trending("mov_1", "mov_2");
        ContinueWatchingItem watching = new ContinueWatchingItem("mov_8832", "ep_102", 1420);
        titles.add("mov_77");
        interactions.store(PROFILE, List.of(watching), List.of("mov_77"));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(HomeSource.FALLBACK, screen.source());
        assertEquals(PROFILE, screen.profileId());
        assertEquals(NOW, screen.generatedAt());
        assertEquals(1, screen.categories().size());
        assertEquals("Trending", screen.categories().get(0).name());
        assertEquals(List.of(summary("mov_1"), summary("mov_2")), screen.categories().get(0).titles());
        assertEquals(List.of(watching), screen.continueWatching());
        assertEquals(List.of(summary("mov_77")), screen.recommended());
        assertEquals(1, homeViews.saveCalls);
        assertEquals(List.of("mov_1", "mov_2"), homeViews.findByProfileId(PROFILE).orElseThrow()
                .categories().get(0).titleIds());
    }

    @Test
    void aCacheThatCannotBeReachedIsTreatedLikeAMissingView() {
        homeViews.down = true;
        titles.trending("mov_1");

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(HomeSource.FALLBACK, screen.source());
        assertEquals(List.of(summary("mov_1")), screen.categories().get(0).titles());
        assertEquals(1, interactions.recommendationCalls);
        assertEquals(1, interactions.continueWatchingCalls);
    }

    @Test
    void whenTheCacheAndTheCatalogAreBothDownTheServiceIsUnavailable() {
        homeViews.down = true;
        titles.down = true;

        ServiceUnavailableException error =
                assertThrows(ServiceUnavailableException.class, () -> service.getHome(PROFILE));

        assertEquals("SERVICE_UNAVAILABLE", error.getCode());
    }

    @Test
    void whenInteractionsDbIsDownACacheMissIsUnavailable() {
        titles.trending("mov_1");
        interactions.down = true;

        ServiceUnavailableException error =
                assertThrows(ServiceUnavailableException.class, () -> service.getHome(PROFILE));

        assertEquals("SERVICE_UNAVAILABLE", error.getCode());
        assertEquals(0, homeViews.saveCalls);
    }

    @Test
    void simultaneousCacheMissesForOneProfileShareTheRebuild() throws Exception {
        CountDownLatch rebuildStarted = new CountDownLatch(1);
        CountDownLatch finishRebuild = new CountDownLatch(1);
        CountDownLatch bothInitialReadsMissed = new CountDownLatch(2);
        AtomicInteger initialReads = new AtomicInteger();
        InMemoryHomeViews concurrentHomeViews = new InMemoryHomeViews() {
            @Override
            public java.util.Optional<HomeView> findByProfileId(String profileId) {
                java.util.Optional<HomeView> view = super.findByProfileId(profileId);
                if (view.isEmpty() && initialReads.incrementAndGet() <= 2) {
                    bothInitialReadsMissed.countDown();
                }
                return view;
            }
        };
        InMemoryInteractions blockingInteractions = new InMemoryInteractions() {
            @Override
            public List<String> findRecommendations(String profileId) {
                super.findRecommendations(profileId);
                rebuildStarted.countDown();
                try {
                    if (!finishRebuild.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to finish the test rebuild");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Test rebuild was interrupted", e);
                }
                return List.of();
            }
        };
        HomeService concurrentService = new HomeService(concurrentHomeViews, titles, blockingInteractions,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(15), Duration.ofHours(24));
        titles.trending("mov_1");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<HomeScreen> first = executor.submit(() -> concurrentService.getHome(PROFILE));
            assertTrue(rebuildStarted.await(2, TimeUnit.SECONDS));
            Future<HomeScreen> second = executor.submit(() -> concurrentService.getHome(PROFILE));
            assertTrue(bothInitialReadsMissed.await(2, TimeUnit.SECONDS));
            finishRebuild.countDown();

            assertEquals(List.of(summary("mov_1")), first.get(2, TimeUnit.SECONDS).categories().get(0).titles());
            assertEquals(List.of(summary("mov_1")), second.get(2, TimeUnit.SECONDS).categories().get(0).titles());
            assertEquals(1, blockingInteractions.recommendationCalls);
            assertEquals(1, concurrentHomeViews.saveCalls);
        } finally {
            finishRebuild.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void aStoredViewCannotBeCompletedWhenTheCatalogIsDown() {
        homeViews.store(new HomeView(PROFILE, GENERATED_AT,
                List.of(new CategoryRef("Trending", List.of("mov_1"))), List.of(), List.of()));
        titles.down = true;

        assertThrows(ServiceUnavailableException.class, () -> service.getHome(PROFILE));
    }

    @Test
    void titlesThatNoLongerExistAreLeftOutKeepingTheOrderOfTheRest() {
        titles.add("mov_1", "mov_3");
        homeViews.store(new HomeView(PROFILE, GENERATED_AT,
                List.of(new CategoryRef("Trending", List.of("mov_1", "mov_2", "mov_3"))), List.of(), List.of()));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(List.of(summary("mov_1"), summary("mov_3")), screen.categories().get(0).titles());
    }

    @Test
    void theCatalogIsAskedOnceAndWithoutRepeatedIds() {
        titles.add("mov_1", "mov_2");
        homeViews.store(new HomeView(PROFILE, GENERATED_AT,
                List.of(new CategoryRef("A", List.of("mov_1", "mov_2")), new CategoryRef("B", List.of("mov_2"))),
                List.of(), List.of("mov_1")));

        service.getHome(PROFILE);

        assertEquals(1, titles.findByIdsCalls);
        assertEquals(List.of("mov_1", "mov_2"), titles.lastRequestedIds);
    }

    @Test
    void aViewWithoutTitleIdsDoesNotQueryTheCatalog() {
        homeViews.store(new HomeView(PROFILE, GENERATED_AT, List.of(), List.of(), List.of()));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(HomeSource.CACHE, screen.source());
        assertEquals(0, titles.findByIdsCalls);
    }

    @Test
    void aViewWithoutCategoriesIsCompletedWithTheTrendingTitlesAndStaysMarkedAsCache() {
        titles.trending("mov_1", "mov_2");
        homeViews.store(new HomeView(PROFILE, GENERATED_AT, List.of(), List.of(), List.of()));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(HomeSource.CACHE, screen.source());
        assertEquals(GENERATED_AT, screen.generatedAt());
        assertEquals(1, screen.categories().size());
        assertEquals("Trending", screen.categories().get(0).name());
        assertEquals(List.of(summary("mov_1"), summary("mov_2")), screen.categories().get(0).titles());
    }

    @Test
    void completingTheTrendingCategoryKeepsTheContinueWatchingAndTheRecommended() {
        titles.add("mov_77");
        titles.trending("mov_1");
        ContinueWatchingItem watching = new ContinueWatchingItem("mov_8832", "ep_102", 1420);
        homeViews.store(new HomeView(PROFILE, GENERATED_AT, List.of(), List.of(watching), List.of("mov_77")));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(List.of(watching), screen.continueWatching());
        assertEquals(List.of(summary("mov_77")), screen.recommended());
        assertEquals(List.of(summary("mov_1")), screen.categories().get(0).titles());
    }

    @Test
    void aViewWithCategoriesDoesNotAskForTheTrendingTitles() {
        titles.add("mov_1");
        homeViews.store(new HomeView(PROFILE, GENERATED_AT,
                List.of(new CategoryRef("Picked for you", List.of("mov_1"))), List.of(), List.of()));

        HomeScreen screen = service.getHome(PROFILE);

        assertEquals(List.of("Picked for you"), screen.categories().stream().map(c -> c.name()).toList());
        assertEquals(0, titles.findTrendingCalls);
    }

    @Test
    void aViewWithoutCategoriesCannotBeCompletedWhenTheCatalogIsDown() {
        homeViews.store(new HomeView(PROFILE, GENERATED_AT, List.of(), List.of(), List.of()));
        titles.down = true;

        assertThrows(ServiceUnavailableException.class, () -> service.getHome(PROFILE));
    }

    @Test
    void anEmptyTrendingCategoryIsNotAdded() {
        homeViews.store(new HomeView(PROFILE, GENERATED_AT, List.of(), List.of(), List.of()));

        HomeScreen screen = service.getHome(PROFILE);

        assertTrue(screen.categories().isEmpty());
    }

    @Test
    void withoutAnActiveProfileTheHomeIsRefused() {
        ProfileNotSelectedException blank =
                assertThrows(ProfileNotSelectedException.class, () -> service.getHome(" "));
        assertThrows(ProfileNotSelectedException.class, () -> service.getHome(null));

        assertEquals("PROFILE_NOT_SELECTED", blank.getCode());
    }
}
