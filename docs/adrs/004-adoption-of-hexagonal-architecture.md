# ADR 004: Adoption of Hexagonal Architecture (Ports and Adapters)

## Status
Accepted

## Context and Problem
StreamHub requires the development of multiple micro-services, including the catalog, playback telemetry, and recommendation modules. Directly coupling business logic to specific frameworks, databases, message brokers, and external services makes the system hard to test, maintain, and evolve safely.

## Alternatives Considered
1. Traditional layered architecture, organizing the application into controllers, services, and repositories.
2. Hexagonal Architecture (Ports and Adapters), organizing the system around domain rules and strict dependency inversion.

## Decision
Adopt Hexagonal Architecture (Ports and Adapters) to separate core business logic from infrastructure concerns. The domain and application layers define business rules and required interfaces, while adapters implement communication with technologies such as PostgreSQL, MongoDB, Redis, and RabbitMQ.

## Trade-offs
- **Positive:** Improved testability through dependency inversion; reduced coupling between business logic and infrastructure; easier replacement of databases or messaging brokers; clearer separation of responsibilities.
- **Negative:** Additional interfaces and abstractions; increased initial implementation effort; potential overengineering for trivial components if applied without discretion.

## Final Justification
Hexagonal Architecture allows StreamHub to isolate its core domain logic. For instance, telemetry can publish events through a defined port without depending directly on RabbitMQ, while adapters handle the integration details, ensuring long-term maintainability and flexibility.