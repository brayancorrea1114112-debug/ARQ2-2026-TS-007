package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.application.ports.out.TitleGenresPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.TitleGenres;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads only the genres of every title of the Catalog DB (collection titles, spec.md 2.3). */
public class MongoTitleGenresAdapter implements TitleGenresPort {

    private static final Logger log = LoggerFactory.getLogger(MongoTitleGenresAdapter.class);

    private final MongoTemplate mongo;

    public MongoTitleGenresAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<TitleGenres> findAll() {
        Query query = new Query();
        query.fields().include("genres");
        List<Document> documents;
        try {
            documents = mongo.find(query, Document.class, MongoTitleSummaryAdapter.COLLECTION);
        } catch (DataAccessException e) {
            throw new ServiceUnavailableException("The catalog", e);
        }
        return documents.stream().map(MongoTitleGenresAdapter::toTitleGenres).flatMap(Optional::stream).toList();
    }

    /** A title without genres is still listed, with none; a document that cannot be read is left out. */
    static Optional<TitleGenres> toTitleGenres(Document document) {
        try {
            List<String> genres = document.getList("genres", String.class);
            return Optional.of(new TitleGenres(Objects.toString(document.get("_id"), null), genres));
        } catch (RuntimeException e) {
            log.warn("Ignoring the genres of the title {}: {}", document.get("_id"), e.getMessage());
            return Optional.empty();
        }
    }
}
