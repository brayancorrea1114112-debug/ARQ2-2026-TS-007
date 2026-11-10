package com.streamhub.telemetry_service.infrastructure.adapters.in.rest;

import com.streamhub.telemetry_service.application.ports.in.IngestTelemetryUseCase;
import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.infrastructure.adapters.in.rest.dto.TelemetryDtos.AcceptedResponse;
import com.streamhub.telemetry_service.infrastructure.adapters.in.rest.dto.TelemetryDtos.PingRequest;
import com.streamhub.telemetry_service.infrastructure.security.AuthenticatedUser;
import com.streamhub.telemetry_service.infrastructure.web.CorrelationIdFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/v1/telemetry/ping (spec.md 3.5): publish and answer 202 without waiting for the worker. */
@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final IngestTelemetryUseCase useCase;

    public TelemetryController(IngestTelemetryUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(value = "/ping", produces = "application/json;charset=UTF-8")
    public ResponseEntity<AcceptedResponse> ping(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PingRequest request,
            @RequestHeader(name = "traceparent", required = false) String traceparent) {
        EventContext context = new EventContext(MDC.get(CorrelationIdFilter.MDC_KEY), traceparent);
        useCase.ingestPing(user.profileId(), request.toCommand(), context);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AcceptedResponse.accepted(request.eventId()));
    }
}
