package com.hjmin.payment.orchestrator.app;

import com.hjmin.payment.orchestrator.domain.*;
import com.hjmin.payment.orchestrator.infra.ExternalPgClient;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrchestratorService {

    private final TransactionRepository txRepo;
    private final TxEventRepository eventRepo;
    private final ExternalPgClient externalPgClient; // 네가 만든 클라이언트 이름에 맞춰 수정

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
}
