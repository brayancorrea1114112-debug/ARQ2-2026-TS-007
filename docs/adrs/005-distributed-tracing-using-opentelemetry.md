# ADR 005: Distributed Tracing and Observability Using OpenTelemetry

## Status
Accepted

## Context and Problem
StreamHub experiences occasional high latency when users initially request the Home screen. Because requests pass through the API Gateway, authentication services, catalog services, Redis, and MongoDB, identifying the exact bottleneck or component responsible for delays is extremely difficult without end-to-end request tracing.

## Alternatives Considered
1. Standard application logs with timestamps and correlation IDs.
2. Distributed tracing using OpenTelemetry with a compatible tracing backend like Jaeger.
3. Infrastructure-level monitoring using standard metrics dashboards without distributed request tracing.

## Decision
Option 2 - Implement OpenTelemetry for distributed tracing and use Jaeger to collect, visualize, and analyze traces across all platform services.

## Trade-offs
- **Positive:** End-to-end visibility into request execution paths; precise identification of latency bottlenecks across services and databases; improved troubleshooting of slow initial requests; correlation of operations across distributed boundaries.
- **Negative:** Additional code instrumentation and infrastructure requirements; storage and processing overhead for trace data; need to configure proper sampling rates.

## Final Justification
Distributed tracing enables StreamHub to pinpoint which microservice or database contributes most to initial client request latency, empowering the engineering team to diagnose bottlenecks and optimize the Home screen delivery path effectively.

## Implementation Notes

This delivery implements **traces only**; the metrics and logs stack is out of scope.

- `otel-collector/otel-collector.yml` receives OTLP over gRPC/HTTP and exports batches to Jaeger (`jaeger:4317`).
- Spring Boot 4 services use `spring-boot-micrometer-tracing-opentelemetry`; the Spring Boot 3 worker uses `micrometer-tracing-bridge-otel` and the OTLP HTTP exporter.
- Kong uses the `opentelemetry` plugin with `KONG_TRACING_INSTRUMENTATIONS=all` and W3C propagation, so it creates and injects `traceparent`.
- RabbitMQ observations propagate the trace context; the worker span joins the HTTP trace of the ping or like.
- Jaeger UI: <http://localhost:16686>.
- Prometheus, Loki, Grafana, Promtail, metrics endpoints and dashboards are deliberately excluded from this change.