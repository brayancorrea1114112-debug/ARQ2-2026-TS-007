package com.streamhub.catalog_service.domain.model;

import com.streamhub.catalog_service.domain.exception.InvalidDomainDataException;

import java.util.List;
import java.util.Objects;

/**
 * A movie or a series of the catalog (spec.md 2.3). It is an entity: two titles are the same
 * title when they have the same id, even if their other data differs.
 *
 * <p>Rule that this class guarantees: a movie has a duration and no seasons, a series has
 * seasons and no duration of its own. The constructor is private, so the only ways to create
 * a title are {@link #movie} and {@link #series}, and an invalid title cannot exist.
 */
public final class Title {

    private final String id;
    private final TitleType type;
    private final String name;
    private final List<String> genres;
    private final List<String> cast;
    private final String ageRating;
    private final String poster;
    private final Integer durationSeconds; // only for MOVIE
    private final List<Season> seasons;    // only for SERIES

    private Title(String id, TitleType type, String name, List<String> genres, List<String> cast,
                  String ageRating, String poster, Integer durationSeconds, List<Season> seasons) {
        this.id = Require.notBlank(id, "title id");
        this.type = Require.notNull(type, "title type");
        this.name = Require.notBlank(name, "title name");
        this.genres = Require.listOrEmpty(genres);
        this.cast = Require.listOrEmpty(cast);
        this.ageRating = ageRating;
        this.poster = poster;
        this.durationSeconds = durationSeconds;
        this.seasons = seasons;
    }

    public static Title movie(String id, String name, List<String> genres, List<String> cast,
                              String ageRating, String poster, int durationSeconds) {
        Require.positive(durationSeconds, "movie duration");
        return new Title(id, TitleType.MOVIE, name, genres, cast, ageRating, poster, durationSeconds, List.of());
    }

    public static Title series(String id, String name, List<String> genres, List<String> cast,
                               String ageRating, String poster, List<Season> seasons) {
        List<Season> checked = Require.nonEmptyList(seasons, "series seasons");
        long distinctNumbers = checked.stream().map(Season::number).distinct().count();
        if (distinctNumbers != checked.size()) {
            throw new InvalidDomainDataException("series seasons must have different numbers");
        }
        return new Title(id, TitleType.SERIES, name, genres, cast, ageRating, poster, null, checked);
    }

    /** The short version of this title that the Home screen shows. */
    public TitleSummary toSummary() {
        return new TitleSummary(id, name, poster);
    }

    public String id() {
        return id;
    }

    public TitleType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public List<String> genres() {
        return genres;
    }

    public List<String> cast() {
        return cast;
    }

    public String ageRating() {
        return ageRating;
    }

    public String poster() {
        return poster;
    }

    /** Duration of a movie. Series have none: their duration is in each episode. */
    public Integer durationSeconds() {
        return durationSeconds;
    }

    /** Seasons of a series. Empty for a movie. */
    public List<Season> seasons() {
        return seasons;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Title that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Title[" + id + ", " + type + ", " + name + "]";
    }
}
