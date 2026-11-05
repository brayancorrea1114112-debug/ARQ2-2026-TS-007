package com.streamhub.catalog_service.domain.model;

import com.streamhub.catalog_service.domain.exception.InvalidDomainDataException;

import java.util.List;

/** Small checks shared by the domain objects. Package-private: it is a tool of the model, not part of it. */
final class Require {

    private Require() {
    }

    static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainDataException(field + " must not be blank");
        }
        return value;
    }

    static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new InvalidDomainDataException(field + " must not be null");
        }
        return value;
    }

    static int positive(int value, String field) {
        if (value <= 0) {
            throw new InvalidDomainDataException(field + " must be greater than 0");
        }
        return value;
    }

    static int zeroOrMore(int value, String field) {
        if (value < 0) {
            throw new InvalidDomainDataException(field + " must be 0 or greater");
        }
        return value;
    }

    /** Immutable copy of the list. A missing list becomes an empty one. */
    static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    /** Immutable copy of the list. It must contain at least one element. */
    static <T> List<T> nonEmptyList(List<T> values, String field) {
        if (values == null || values.isEmpty()) {
            throw new InvalidDomainDataException(field + " must contain at least one element");
        }
        return List.copyOf(values);
    }
}
