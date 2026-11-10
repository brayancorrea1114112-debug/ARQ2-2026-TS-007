package com.streamhub.catalog_service.application.usecases;

import com.streamhub.catalog_service.application.ports.in.GetHomeUseCase;
import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.HomeCategory;
import com.streamhub.catalog_service.domain.model.HomeScreen;
import com.streamhub.catalog_service.domain.model.HomeSource;
import com.streamhub.catalog_service.domain.model.HomeView;
import com.streamhub.catalog_service.domain.model.TitleSummary;
import com.streamhub.catalog_service.domain.model.CategoryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the Home screen (spec.md 3.4, Feature 2):
 * <ol>
 *   <li>If a view is in Redis, it is answered with names and posters ({@code CACHE}).</li>
 *   <li>On a miss, the view is rebuilt from MongoDB and Cassandra, cached, and returned as {@code FALLBACK}.</li>
 *   <li>If Redis is down, the rebuilt view is still returned as {@code FALLBACK}.</li>
 * </ol>
 */
public class HomeService implements GetHomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);
    private static final String TRENDING_CATEGORY = "Trending";
    private static final int FALLBACK_SIZE = 20;

    private final HomeViewCachePort homeViews;
    private final TitleSummaryPort titles;
    private final InteractionsReadPort interactions;
    private final Clock clock;
    private final Duration homeTtl;
    private final Duration continueWatchingTtl;
    private final Map<String, CompletableFuture<HomeScreen>> rebuildsInFlight = new ConcurrentHashMap<>();

    public HomeService(HomeViewCachePort homeViews, TitleSummaryPort titles, InteractionsReadPort interactions,
                       Clock clock, Duration homeTtl, Duration continueWatchingTtl) {
        this.homeViews = homeViews;
        this.titles = titles;
        this.interactions = interactions;
        this.clock = clock;
        this.homeTtl = homeTtl;
        this.continueWatchingTtl = continueWatchingTtl;
    }

    @Override
    public HomeScreen getHome(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new ProfileNotSelectedException();
        }
        Optional<HomeView> cached = readView(profileId);
        return cached.map(view -> fromView(view, HomeSource.CACHE))
                .orElseGet(() -> rebuildOnce(profileId));
    }

    /** A cache that cannot be reached is treated like a miss and rebuilt from the source stores. */
    private Optional<HomeView> readView(String profileId) {
        try {
            return homeViews.findByProfileId(profileId);
        } catch (ServiceUnavailableException e) {
            return Optional.empty();
        }
    }

    private HomeScreen rebuildOnce(String profileId) {
        CompletableFuture<HomeScreen> current = new CompletableFuture<>();
        CompletableFuture<HomeScreen> inFlight = rebuildsInFlight.putIfAbsent(profileId, current);
        if (inFlight != null) {
            return await(inFlight);
        }

        try {
            Optional<HomeView> cached = readView(profileId);
            HomeScreen screen = cached.map(view -> fromView(view, HomeSource.CACHE))
                    .orElseGet(() -> rebuild(profileId));
            current.complete(screen);
            return screen;
        } catch (RuntimeException e) {
            current.completeExceptionally(e);
            throw e;
        } finally {
            rebuildsInFlight.remove(profileId, current);
        }
    }

    private static HomeScreen await(CompletableFuture<HomeScreen> inFlight) {
        try {
            return inFlight.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    private HomeScreen rebuild(String profileId) {
        List<TitleSummary> trending = titles.findTrending(FALLBACK_SIZE);
        List<String> recommendations = interactions.findRecommendations(profileId);
        HomeView view = new HomeView(profileId, clock.instant(),
                trending.isEmpty() ? List.of() : List.of(new CategoryRef(TRENDING_CATEGORY,
                        trending.stream().map(TitleSummary::contentId).toList())),
                interactions.findContinueWatching(profileId), recommendations);

        try {
            homeViews.save(view, homeTtl, continueWatchingTtl);
            return fromView(view, HomeSource.FALLBACK);
        } catch (ServiceUnavailableException e) {
            log.warn("Could not cache the rebuilt Home view for {}: {}", profileId, e.getMessage());
            return fromView(view, HomeSource.FALLBACK);
        }
    }

    private HomeScreen fromView(HomeView view, HomeSource source) {
        Map<String, TitleSummary> known = lookup(view.displayedTitleIds());
        List<HomeCategory> categories = view.categories().stream()
                .map(category -> new HomeCategory(category.name(), pick(category.titleIds(), known)))
                .toList();
        return new HomeScreen(view.profileId(), view.generatedAt(), source,
                view.categories().isEmpty() ? trendingCategory() : categories,
                view.continueWatching(), pick(view.recommended(), known));
    }

    private List<HomeCategory> trendingCategory() {
        List<TitleSummary> trending = titles.findTrending(FALLBACK_SIZE);
        return trending.isEmpty() ? List.of() : List.of(new HomeCategory(TRENDING_CATEGORY, trending));
    }

    /** One query for all the ids of the view. With no ids there is nothing to ask. */
    private Map<String, TitleSummary> lookup(List<String> titleIds) {
        if (titleIds.isEmpty()) {
            return Map.of();
        }
        Map<String, TitleSummary> byId = new HashMap<>();
        for (TitleSummary summary : titles.findByIds(titleIds)) {
            byId.putIfAbsent(summary.contentId(), summary);
        }
        return byId;
    }

    /** The summaries of the ids that exist, in the order of the ids. */
    private static List<TitleSummary> pick(List<String> titleIds, Map<String, TitleSummary> known) {
        return titleIds.stream().map(known::get).filter(Objects::nonNull).toList();
    }
}
