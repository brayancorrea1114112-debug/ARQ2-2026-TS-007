package com.streamhub.catalog_service.domain.model;

import com.streamhub.catalog_service.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleTest {

    private static final Season SEASON_ONE = new Season(1, List.of(
            new Episode("ep_101", "Pilot", 3000),
            new Episode("ep_102", "Second", 2900)));

    @Test
    void aMovieHasADurationAndNoSeasons() {
        Title movie = Title.movie("mov_1", "Movie One", List.of("Drama"), List.of("Actor One"),
                "PG-13", "https://cdn.example.com/p/mov_1.jpg", 5400);

        assertEquals(TitleType.MOVIE, movie.type());
        assertEquals(5400, movie.durationSeconds().intValue());
        assertTrue(movie.seasons().isEmpty());
    }

    @Test
    void aSeriesHasSeasonsAndNoDurationOfItsOwn() {
        Title series = Title.series("mov_8832", "Example Series", List.of("Drama", "Thriller"),
                List.of("Actor One", "Actor Two"), "PG-13", "https://cdn.example.com/p/mov_8832.jpg",
                List.of(SEASON_ONE));

        assertEquals(TitleType.SERIES, series.type());
        assertNull(series.durationSeconds());
        assertEquals(1, series.seasons().size());
    }

    @Test
    void aMovieWithoutPositiveDurationIsRejected() {
        assertThrows(InvalidDomainDataException.class,
                () -> Title.movie("mov_1", "Movie One", List.of(), List.of(), "PG", null, 0));
    }

    @Test
    void aSeriesWithoutSeasonsIsRejected() {
        assertThrows(InvalidDomainDataException.class,
                () -> Title.series("ser_1", "Series", List.of(), List.of(), "PG", null, List.of()));
    }

    @Test
    void aSeriesCannotRepeatASeasonNumber() {
        Season repeated = new Season(1, List.of(new Episode("ep_201", "Other", 1000)));

        assertThrows(InvalidDomainDataException.class,
                () -> Title.series("ser_1", "Series", List.of(), List.of(), "PG", null, List.of(SEASON_ONE, repeated)));
    }

    @Test
    void aTitleNeedsAnIdAndAName() {
        assertThrows(InvalidDomainDataException.class,
                () -> Title.movie(" ", "Movie One", List.of(), List.of(), "PG", null, 5400));
        assertThrows(InvalidDomainDataException.class,
                () -> Title.movie("mov_1", null, List.of(), List.of(), "PG", null, 5400));
    }

    @Test
    void aSeasonNeedsEpisodesAndEpisodesNeedADuration() {
        assertThrows(InvalidDomainDataException.class, () -> new Season(1, List.of()));
        assertThrows(InvalidDomainDataException.class, () -> new Episode("ep_1", "Pilot", 0));
    }

    @Test
    void twoTitlesWithTheSameIdAreTheSameTitle() {
        Title first = Title.movie("mov_1", "Movie One", List.of(), List.of(), "PG", null, 5400);
        Title renamed = Title.movie("mov_1", "Movie One (remastered)", List.of(), List.of(), "PG", null, 5400);
        Title other = Title.movie("mov_2", "Movie One", List.of(), List.of(), "PG", null, 5400);

        assertEquals(first, renamed);
        assertNotEquals(first, other);
    }

    @Test
    void theSummaryKeepsIdNameAndPoster() {
        Title movie = Title.movie("mov_1", "Movie One", List.of(), List.of(), "PG",
                "https://cdn.example.com/p/mov_1.jpg", 5400);

        TitleSummary summary = movie.toSummary();

        assertEquals("mov_1", summary.contentId());
        assertEquals("Movie One", summary.name());
        assertEquals("https://cdn.example.com/p/mov_1.jpg", summary.poster());
    }

    @Test
    void theListsOfATitleCannotBeChangedFromOutside() {
        Title movie = Title.movie("mov_1", "Movie One", List.of("Drama"), List.of(), "PG", null, 5400);

        assertThrows(UnsupportedOperationException.class, () -> movie.genres().add("Comedy"));
    }
}
