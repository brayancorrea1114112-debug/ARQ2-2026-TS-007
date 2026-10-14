# ADR 003: Catalog Storage in a Document-Oriented Database (MongoDB)

## Status
Accepted

## Context and Problem
Movie and series metadata have hierarchical and variable structures (seasons, episodes, actors, genres, age ratings) that require multiple normalized tables and complex JOIN operations when queried in a relational SQL database.

## Alternatives Considered
1. Highly normalized schema in PostgreSQL.
2. Document-oriented NoSQL model in MongoDB.
3. Static JSON files loaded into S3 / Object Storage.

## Decision
Option 2 - Use MongoDB for the master catalog of VOD content.

## Trade-offs
- **Positive:** Native JSON format ready to be delivered to clients or cached; fast reads without multi-table joins; schema flexibility for new content attributes.
- **Negative:** Lack of complex ACID transactions across distributed collections (which is not required for read-heavy catalog operations).

## Final Justification
It simplifies VOD catalog data modeling and accelerates integration with the Redis caching and rendering layer.