package com.example.looplock_engine.service;

import com.example.looplock_engine.algorithm.CycleDetectionEngine;
import com.example.looplock_engine.algorithm.TemporalEdge;
import com.example.looplock_engine.entity.FraudRingAlert;
import com.example.looplock_engine.entity.TransactionRecord;
import com.example.looplock_engine.repository.FraudRingAlertRepository;
import com.example.looplock_engine.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestration facade for the LoopLock fraud ring detection pipeline.
 *
 * Coordinates transaction fetching, vertex mapping, cycle detection, risk scoring,
 * and alert persistence within a single transaction boundary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudOrchestrationService {

    private final TransactionRecordRepository transactionRepo;
    private final FraudRingAlertRepository    alertRepo;
    private final CycleDetectionEngine        detectionEngine;
    private final RiskScoringService          scoringService;

    /**
     * Executes a full fraud ring detection scan over all unprocessed transactions.
     */
    @Transactional
    public List<FraudRingResult> runScan() {
        List<TransactionRecord> batch = transactionRepo.findByProcessedFalse();
        log.info("LoopLock scan started: {} unprocessed transactions.", batch.size());
        return executeScan(batch);
    }

    /**
     * Scans only unprocessed transactions within a closed time window.
     * Useful for scheduled incremental scans without re-processing historical data.
     */
    @Transactional
    public List<FraudRingResult> runWindowedScan(Instant windowStart, Instant windowEnd) {
        List<TransactionRecord> batch = transactionRepo.findUnprocessedInWindow(windowStart, windowEnd);
        log.info("LoopLock windowed scan [{} → {}]: {} transactions.", windowStart, windowEnd, batch.size());
        return executeScan(batch);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<FraudRingResult> executeScan(List<TransactionRecord> batch) {
        if (batch.isEmpty()) return Collections.emptyList();

        AccountVertexMapper     mapper   = new AccountVertexMapper();
        List<TemporalEdge>      edges    = mapper.buildEdges(batch);
        List<List<Integer>>     rawRings = detectionEngine.detect(edges, mapper.vertexCount());

        if (rawRings.isEmpty()) {
            log.info("LoopLock scan complete: 0 fraud rings detected.");
            markProcessed(batch);
            return Collections.emptyList();
        }

        Map<String, List<TransactionRecord>> bySource = batch.stream()
                .collect(Collectors.groupingBy(TransactionRecord::getSourceAccountId));

        List<FraudRingResult> results = new ArrayList<>(rawRings.size());
        for (List<Integer> vertexRing : rawRings) {
            List<String> accounts = mapper.resolveAccounts(vertexRing);
            Set<String>  ringSet  = new HashSet<>(accounts);

            List<TransactionRecord> evidence = accounts.stream()
                    .flatMap(src -> bySource.getOrDefault(src, List.of()).stream())
                    .filter(r -> ringSet.contains(r.getDestinationAccountId()))
                    .distinct()
                    .collect(Collectors.toList());

            double riskScore = scoringService.score(evidence.isEmpty() ? batch : evidence);
            results.add(new FraudRingResult(accounts, evidence, riskScore));
            persistAlert(accounts, evidence, riskScore);
        }

        markProcessed(batch);
        log.info("LoopLock scan complete: {} fraud ring(s) detected.", results.size());
        return results;
    }

    private void persistAlert(List<String> accounts, List<TransactionRecord> evidence, double riskScore) {
        String ringIdentifier = accounts.stream().sorted().collect(Collectors.joining("|"));
        BigDecimal totalVolume = evidence.stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FraudRingAlert alert = new FraudRingAlert(
                ringIdentifier, totalVolume, accounts.size(),
                BigDecimal.valueOf(riskScore), accounts, Instant.now());

        alertRepo.save(alert);
        log.warn("FRAUD RING PERSISTED — id={} accounts=[{}] volume={} score={}",
                 alert.getId(), ringIdentifier, totalVolume, String.format("%.2f", riskScore));
    }

    private void markProcessed(List<TransactionRecord> records) {
        records.forEach(r -> r.setProcessed(true));
        transactionRepo.saveAll(records);
    }
}
