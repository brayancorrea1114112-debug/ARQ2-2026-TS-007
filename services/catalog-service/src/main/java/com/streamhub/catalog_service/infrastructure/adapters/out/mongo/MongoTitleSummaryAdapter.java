package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.TitleSummary;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads title summaries from the Catalog DB (collection titles, spec.md 2.3). Only the fields the Home shows
 * are fetched. The trending titles are the ones with a trending_rank, from the lowest rank up: it is an
 * optional field of the catalog document that whoever maintains the catalog fills in.
 */
public class MongoTitleSummaryAdapter implements TitleSummaryPort {

    private static final Logger log = LoggerFactory.getLogger(MongoTitleSummaryAdapter.class);
    static final String COLLECTION = "titles";
    static final String TRENDING_FIELD = "trending_rank";

    private final MongoTemplate mongo;

    public MongoTitleSummaryAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<TitleSummary> findByIds(List<String> titleIds) {
        return read(new Query(Criteria.where("_id").in(titleIds)));
    }

    @Override
    public List<TitleSummary> findTrending(int limit) {
        return read(new Query(Criteria.where(TRENDING_FIELD).exists(true))
                .with(Sort.by(Sort.Direction.ASC, TRENDING_FIELD))
                .limit(limit));
    }

    private List<TitleSummary> read(Query query) {
        query.fields().include("name", "images.poster");
        List<Document> documents;
        try {
            documents = mongo.find(query, Document.class, COLLECTION);
        } catch (DataAccessException e) {
            throw new ServiceUnavailableException("The catalog", e);
        }
        return documents.stream().map(MongoTitleSummaryAdapter::toSummary).flatMap(Optional::stream).toList();
    }

    /** A document without a name cannot be shown: it is left out instead of failing the whole Home. */
    static Optional<TitleSummary> toSummary(Document document) {
        try {
            Document images = document.get("images", Document.class);
            String poster = images == null ? null : images.getString("poster");
            return Optional.of(new TitleSummary(Objects.toString(document.get("_id"), null), document.getString("name"), poster));
        } catch (RuntimeException e) {
            log.warn("Ignoring the title {} with unreadable data: {}", document.get("_id"), e.getMessage());
            return Optional.empty();
        }
    }
}
