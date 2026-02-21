package com.hjmin.payment.orchestrator.app;

import com.hjmin.payment.orchestrator.api.CancelResponse;
import com.hjmin.payment.orchestrator.domain.*;
import com.hjmin.payment.orchestrator.infra.ExternalPgClient;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelRequest;
import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentOrchestratorService {

    private final TransactionRepository txRepo;
    private final TxEventRepository eventRepo;
    private final ExternalPgClient externalPgClient;

    public PaymentOrchestratorService(TransactionRepository txRepo,
                                      TxEventRepository eventRepo,
                                      ExternalPgClient externalPgClient) {
        this.txRepo = txRepo;
        this.eventRepo = eventRepo;
        this.externalPgClient = externalPgClient;
    }

    @Transactional
    public Transaction authorize(String idempotencyKey, String merchantId, long amount, String currency) {

        // 1) 멱등 처리
        var existing = txRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        // 2) CREATED + 이벤트
        Transaction tx = Transaction.create(merchantId, amount, currency, idempotencyKey);
        tx = txRepo.save(tx);
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CREATED, "Transaction created"));

        // 3) ROUTE + 이벤트
        tx.routedTo("PG-MOCK");
        txRepo.save(tx); // 여기서 ROUTED_TO_EXTERNAL + selectedPg + routedAt 확정
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.ROUTED_TO_EXTERNAL, "Routed to PG-MOCK"));

        // 4) 외부 호출
        var pgRes = externalPgClient.authorize(
                new ExternalPgClient.PgRequest(tx.getMerchantId(), tx.getAmount(), tx.getCurrency())
        );

        // 4-1) EXTERNAL_RESPONSE 상태 + 이벤트
        tx.applyExternalResponse(pgRes.resultCode(), pgRes.approvalNo());
        txRepo.save(tx); // 여기서 pgResultCode/approvalNo + status(EXTERNAL_RESPONSE) 확정
        eventRepo.save(TxEvent.of(
                tx.getId(),
                TxEventType.EXTERNAL_RESPONSE,
                "PG resultCode=" + pgRes.resultCode() + ", approvalNo=" + pgRes.approvalNo()
        ));

        if ("00".equals(pgRes.resultCode())) {
            tx.authorized(pgRes.resultCode(), pgRes.approvalNo());
            txRepo.save(tx);
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.AUTHORIZED, "Approved"));
        } else {
            tx.failed(pgRes.resultCode(), pgRes.message());
            txRepo.save(tx);
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.FAILED, "Declined: " + pgRes.message()));
        }


        return txRepo.save(tx);
    }

    @Transactional
    public CancelResponse cancel(UUID txId, String idempotencyKey) {
        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("tx not found: " + txId));

        tx.assertCancelable();

        // 이미 취소가 종결(CANCELED/CANCEL_FAILED)이라면 그대로 반환 = 멱등
        if (tx.isCancelTerminal()) {
            return new CancelResponse(tx.getId(), tx.getStatus().name(), tx.getCancelResultCode(), tx.getCancelReason());
        }

        // 진행 중(CANCEL_REQUESTED)인 경우: 같은 키면 그대로 진행/반환, 다른 키면 정책상 차단
        tx.verifyCancelIdempotencyKey(idempotencyKey);

        // AUTHORIZED -> CANCEL_REQUESTED
        if (tx.getStatus() == TxStatus.AUTHORIZED) {
            tx.requestCancel(idempotencyKey);
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CANCEL_REQUESTED, "cancel requested"));
        }

        // 외부 취소 라우팅 이벤트
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.ROUTED_CANCEL_TO_EXTERNAL, "route cancel to " + tx.getSelectedPg()));

        PgCancelResponse pgRes;
        try {
            pgRes = externalPgClient.cancel(new PgCancelRequest(
                    tx.getId(), tx.getMerchantId(), tx.getAmount(), tx.getCurrency()
            ));
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.EXTERNAL_CANCEL_RESPONSE, "code=" + pgRes.resultCode()));
        } catch (Exception e) {
            tx.cancelFailed("NETWORK_ERROR", e.getClass().getSimpleName() + ": " + e.getMessage());
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CANCEL_FAILED, "exception=" + e.getClass().getSimpleName()));
            return new CancelResponse(tx.getId(), tx.getStatus().name(), tx.getCancelResultCode(), tx.getCancelReason());
        }

        if (pgRes.approved()) {
            tx.canceled(pgRes.resultCode(), pgRes.message());
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CANCELED, "canceled"));
        } else {
            tx.cancelFailed(pgRes.resultCode(), pgRes.message());
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CANCEL_FAILED, "cancel denied"));
        }

        return new CancelResponse(tx.getId(), tx.getStatus().name(), tx.getCancelResultCode(), tx.getCancelReason());
    }
}
