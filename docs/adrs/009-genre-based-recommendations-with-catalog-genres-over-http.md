# ADR 009: Genre-Based Recommendations with Catalog Genres Read over HTTP

## Status
Accepted

## Context and Problem
Feature 4 requires that likes and dislikes feed the suggestion engine. The first strategy of the Recommendation Worker only used the interactions of the profile: it suggested the titles the profile had liked and not started. It never suggested a new title, so a like on a title the profile had already watched left the "Recommended" row empty. The preference vector stored in the Interactions DB (`prefs_by_profile`) was written but never read.

Suggesting new titles needs to know which titles are similar. The genres of the titles belong to the Catalog & Home Service (MongoDB, ADR 003), not to the worker, and each service owns its database.

## Alternatives Considered
1. The worker reads the genres directly from the Catalog DB (MongoDB).
2. The Catalog & Home Service publishes an event whenever a title changes, and the worker keeps its own copy of the genres.
3. The Catalog & Home Service exposes the genres through an internal HTTP endpoint, and the worker reads them and keeps them in memory for a few minutes.

## Decision
Option 3.
- The Catalog & Home Service exposes `GET /internal/v1/titles/genres` (spec.md 3.7). Kong does not route `/internal`, so the endpoint is only reachable inside the Docker network. It needs no token because it only returns public catalog data.
- The worker reads the genres through the `CatalogGenresPort` and keeps them in memory for 5 minutes (`CATALOG_GENRES_CACHE_TTL`). The call has a 2-second timeout.
- The `GenreAffinityRecommendationStrategy` turns the preference vector into genre weights: each rated title moves its genres by its weight (LIKE +1, DISLIKE -1) and each started title adds 0.5. It suggests the titles the profile has neither rated nor started, ordered by the sum of the weights of their genres, keeping only positive scores (at most 20).
- If the catalog cannot be reached, the last copy of the genres is used even if it expired. Without any copy, the worker still persists the event and keeps the previous recommendations: the message is not retried, because retrying would not help while the catalog is down.

## Trade-offs
- Positive: Feature 4 now suggests new titles based on likes, dislikes and viewing; the Catalog & Home Service remains the only owner of the catalog data; the preference vector of the Interactions DB has a purpose; no new infrastructure is needed; the call is part of the trace of the event (ADR 005).
- Negative: a synchronous dependency between two services appears (mitigated by the timeout, the in-memory copy and keeping the previous recommendations); a genre change reaches the recommendations only after the cache expires (acceptable, genres rarely change); the endpoint returns the whole catalog, which is fine for the current size but would need paging for a large catalog; the strategy is simple and does not learn from other profiles (it can be replaced through the `RecommendationStrategy` port, ADR 004).

## Final Justification
Reading the genres through the Catalog & Home Service respects the ownership of its data, which option 1 breaks. Events (option 2) are the right tool for state that changes and must be propagated, but the catalog is loaded once and rarely changes, so a copy kept up to date by events would add a table and a consumer for almost no benefit. A cached read gives the worker the data it needs with a bounded and well-understood failure mode.
