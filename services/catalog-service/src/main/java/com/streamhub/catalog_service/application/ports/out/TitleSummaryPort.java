package com.streamhub.catalog_service.application.ports.out;

import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.util.List;

/**
 * Where the summaries of the catalog titles are read from. Whether they come straight from MongoDB
 * or from a cache in front of it is decided by the adapter, not by the use cases.
 *
 * @throws com.streamhub.catalog_service.domain.exception.ServiceUnavailableException (both methods)
 *         when the catalog cannot be reached
 */
public interface TitleSummaryPort {

    /** Summaries of the given ids. An id that does not exist is simply missing from the answer; the order is not guaranteed. */
    List<TitleSummary> findByIds(List<String> titleIds);

    /** The most watched titles, used to build the generic Home when a profile has no precomputed view. */
    List<TitleSummary> findTrending(int limit);
}
