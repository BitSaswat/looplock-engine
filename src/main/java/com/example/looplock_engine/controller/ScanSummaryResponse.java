package com.example.looplock_engine.controller;

import com.example.looplock_engine.service.FraudRingResult;

import java.util.List;

/**
 * Response envelope for a completed fraud scan execution.
 * Belongs in the controller package — it is an HTTP response shape,
 * not a service-layer concern.
 */
public record ScanSummaryResponse(
        int ringsDetected,
        String message,
        List<FraudRingResult> rings
) {
    public static ScanSummaryResponse of(List<FraudRingResult> results) {
        return new ScanSummaryResponse(
                results.size(),
                results.isEmpty()
                        ? "Scan complete. No fraud rings detected."
                        : String.format("Scan complete. %d fraud ring(s) detected.", results.size()),
                results
        );
    }
}
