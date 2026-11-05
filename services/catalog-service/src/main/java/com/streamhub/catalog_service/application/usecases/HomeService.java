package com.streamhub.catalog_service.application.usecases;

import com.streamhub.catalog_service.application.ports.in.GetHomeUseCase;
import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.HomeCategory;
import com.streamhub.catalog_service.domain.model.HomeScreen;
import com.streamhub.catalog_service.domain.model.HomeSource;
import com.streamhub.catalog_service.domain.model.HomeView;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the Home screen (spec.md 3.4, Feature 2):
 * <ol>
 *   <li>If the precomputed view is in the cache, it is answered with names and posters ({@code CACHE}).</li>
 *   <li>If it is not, or the cache is down, a generic Home with the trending titles is answered ({@code FALLBACK}).</li>
 *   <li>If the catalog is down too, {@link ServiceUnavailableException} is thrown.</li>
 * </ol>
 * It never builds recommendations: that is the work of the Recommendation Worker.
 */
public class HomeService implements GetHomeUseCase {

    private static final String TRENDING_CATEGORY = "Trending";
    private static final int FALLBACK_SIZE = 20;

    private final HomeViewCachePort homeViews;
    private final TitleSummaryPort titles;
    private final Clock clock;

    public HomeService(HomeViewCachePort homeViews, TitleSummaryPort titles, Clock clock) {
        this.homeViews = homeViews;
        this.titles = titles;
        this.clock = clock;
    }

    @Override
    public HomeScreen getHome(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new ProfileNotSelectedException();
        }
        return readView(profileId)
                .map(this::fromView)
                .orElseGet(() -> fallback(profileId));
    }

    /** A cache that cannot be reached is treated like a missing view: the Home is built from the catalog instead. */
    private Optional<HomeView> readView(String profileId) {
        try {
            return homeViews.findByProfileId(profileId);
        } catch (ServiceUnavailableException e) {
            return Optional.empty();
        }
    }

    private HomeScreen fromView(HomeView view) {
        Map<String, TitleSummary> known = lookup(view.displayedTitleIds());
        List<HomeCategory> categories = view.categories().stream()
                .map(category -> new HomeCategory(category.name(), pick(category.titleIds(), known)))
                .toList();
        return new HomeScreen(view.profileId(), view.generatedAt(), HomeSource.CACHE,
                view.categories().isEmpty() ? trendingCategory() : categories,
                view.continueWatching(), pick(view.recommended(), known));
    }

    /**
     * The Recommendation Worker does not build categories, so a view without them is completed here with
     * "Trending". A catalog without titles gives no category at all, instead of an empty one.
     */
    private List<HomeCategory> trendingCategory() {
        List<TitleSummary> trending = titles.findTrending(FALLBACK_SIZE);
        return trending.isEmpty() ? List.of() : List.of(new HomeCategory(TRENDING_CATEGORY, trending));
    }

    private HomeScreen fallback(String profileId) {
        List<TitleSummary> trending = titles.findTrending(FALLBACK_SIZE);
        return new HomeScreen(profileId, clock.instant(), HomeSource.FALLBACK,
                List.of(new HomeCategory(TRENDING_CATEGORY, trending)), List.of(), List.of());
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
