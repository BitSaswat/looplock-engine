package com.example.looplock_engine.controller;

import com.example.looplock_engine.entity.FraudRingAlert;
import com.example.looplock_engine.repository.FraudRingAlertRepository;
import com.example.looplock_engine.service.FraudOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST presentation layer for the LoopLock fraud ring detection engine.
 *
 * Delegation layer orchestrating scans and retrieving alerts.
 * Swagger UI available at {@code /swagger-ui.html} after startup.
 */
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(
    name        = "Fraud Detection",
    description = "Endpoints for triggering the LoopLock fraud ring detection engine " +
                  "and retrieving persisted alert results. The engine uses a two-phase " +
                  "DSU pre-filter + Tarjan's SCC algorithm to detect directed " +
                  "transaction cycles with amortized O(α(N)) complexity."
)
public class FraudController {

    private final FraudOrchestrationService orchestrationService;
    private final FraudRingAlertRepository  alertRepository;

    // -------------------------------------------------------------------------
    // POST /api/v1/fraud/scan
    // -------------------------------------------------------------------------

    @PostMapping("/scan")
    @Operation(
        summary     = "Trigger a full fraud ring detection scan",
        description = "Fetches unprocessed transactions, runs the detection pipeline, scores rings, and persists alerts."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scan executed successfully; result contains 0..N detected rings"),
        @ApiResponse(responseCode = "500", description = "Internal engine failure; check server logs for stack trace")
    })
    public ResponseEntity<ScanSummaryResponse> triggerScan() {
        return ResponseEntity.ok(ScanSummaryResponse.of(orchestrationService.runScan()));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/fraud/scan/windowed
    // -------------------------------------------------------------------------

    @PostMapping("/scan/windowed")
    @Operation(
        summary     = "Trigger a time-windowed fraud ring detection scan",
        description = "Scans unprocessed transactions within the specified epoch-millisecond range."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Windowed scan executed successfully"),
        @ApiResponse(responseCode = "400", description = "windowStart is after windowEnd")
    })
    public ResponseEntity<ScanSummaryResponse> triggerWindowedScan(
            @Parameter(description = "Window start as epoch milliseconds", required = true)
            @RequestParam long windowStartMs,
            @Parameter(description = "Window end as epoch milliseconds", required = true)
            @RequestParam long windowEndMs) {

        if (windowStartMs > windowEndMs) {
            return ResponseEntity.badRequest().build();
        }

        var windowStart = java.time.Instant.ofEpochMilli(windowStartMs);
        var windowEnd   = java.time.Instant.ofEpochMilli(windowEndMs);

        return ResponseEntity.ok(
                ScanSummaryResponse.of(orchestrationService.runWindowedScan(windowStart, windowEnd)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/fraud/alerts
    // -------------------------------------------------------------------------

    @GetMapping("/alerts")
    @Operation(
        summary     = "Retrieve persisted fraud ring alerts",
        description = "Returns a paginated list of alerts, sorted by risk score descending by default."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alert page returned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<FraudRingAlert>> getAlerts(
            @ParameterObject
            @PageableDefault(size = 20, sort = "riskScore", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(alertRepository.findAll(pageable));
    }
}
