package com.hjmin.payment.orchestrator.batch;

import com.hjmin.payment.orchestrator.domain.Transaction;
import com.hjmin.payment.orchestrator.domain.TxEvent;
import com.hjmin.payment.orchestrator.domain.TxEventType;
import com.hjmin.payment.orchestrator.domain.TxStatus;
import com.hjmin.payment.orchestrator.infra.PgMockQueryClient;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ReconciliationJob {

    private final TransactionRepository txRepo;
    private final TxEventRepository eventRepo;
    private final PgMockQueryClient pgMockQueryClient;

    // 비교 대상: 외부 연동이 끝났는데도 상태가 이상하거나, 취소 진행 중 등
    private static final EnumSet<TxStatus> TARGET = EnumSet.of(
            TxStatus.AUTHORIZED,
            TxStatus.FAILED,
            TxStatus.CANCEL_REQUESTED,
            TxStatus.CANCELED,
            TxStatus.CANCEL_FAILED
    );

    private static final EnumSet<TxEventType> RECON_RESULT_TYPES = EnumSet.of
            (TxEventType.RECON_MATCH, TxEventType.RECON_MISMATCH);

    // 데모용: 30초마다 실행
    @Scheduled(fixedDelay = 30_000)
    public void run() {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(30));

        List<Transaction> txs = txRepo.findRecentTargets(from, TARGET);

        UUID runId = UUID.randomUUID(); // runId는 배치 실행 단위로 1번만 (변화가 있을 때만 기록하기 위함)

        for (Transaction tx : txs) {

            String resultMessage;      // 비교 결과 메시지(저장 여부 판단 기준)
            TxEventType resultType;    // MATCH or MISMATCH

            PgMockQueryClient.PgTxStatusResponse ext;
            try {
                ext = pgMockQueryClient.getTxStatus(tx.getId());
            } catch (Exception e) {
                resultType = TxEventType.RECON_MISMATCH;
                resultMessage = "recon mismatch: txId=" + tx.getId() + ", external error=" + e.getClass().getSimpleName();
                if (shouldSkip(tx.getId(), resultType, resultMessage)) continue;

                eventRepo.save(TxEvent.of(tx.getId(), TxEventType.RECON_STARTED, "recon start runId=" + runId));
                eventRepo.save(TxEvent.of(tx.getId(), resultType, resultMessage));
                continue;
            }

            String expected = mapToExternalState(tx.getStatus());
            if (!expected.equals(ext.state())) {
                resultType = TxEventType.RECON_MISMATCH;
                resultMessage = "recon mismatch: internal=" + tx.getStatus() + ", external=" + ext.state() + ", code=" + ext.resultCode();
            } else {
                resultType = TxEventType.RECON_MATCH;
                resultMessage = "recon match: " + ext.state() + ", code=" + ext.resultCode();
            }

            if (shouldSkip(tx.getId(), resultType, resultMessage)) continue;

            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.RECON_STARTED, "recon start runId=" + runId));
            eventRepo.save(TxEvent.of(tx.getId(), resultType, resultMessage));
        }
    }

    private boolean shouldSkip(UUID txId, TxEventType newType, String newMessage) {
        return eventRepo.findTopByTxIdAndEventTypeInOrderByCreatedAtDesc(txId, RECON_RESULT_TYPES)
                .map(last -> last.getEventType() == newType && Objects.equals(last.getMessage(), newMessage))
                .orElse(false);
    }

    private String mapToExternalState(TxStatus internal) {
        // MVP 매핑: pg-mock state 이름과 맞추기
        return switch (internal) {
            case AUTHORIZED -> "AUTHORIZED";
            case FAILED -> "FAILED";
            case CANCELED -> "CANCELED";
            case CANCEL_FAILED -> "CANCEL_FAILED";
            case CANCEL_REQUESTED -> "CANCELED"; // 정책: 요청 중이면 외부는 이미 취소됐을 수도/아닐 수도
            default -> "UNKNOWN";
        };
    }
}
