package com.hjmin.payment.orchestrator.api;

import com.hjmin.payment.orchestrator.app.PaymentOrchestratorService;
import com.hjmin.payment.orchestrator.domain.Transaction;
import com.hjmin.payment.orchestrator.domain.TxEvent;
import com.hjmin.payment.orchestrator.domain.TxEventType;
import com.hjmin.payment.orchestrator.domain.TxStatus;
import com.hjmin.payment.orchestrator.infra.ExternalPgClient;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelRequest;
import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelResponse;
import com.zaxxer.hikari.pool.HikariProxyCallableStatement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApiController {

    private final TransactionRepository txRepo;
    private final TxEventRepository eventRepo;
    private final ExternalPgClient externalPgClient;

    private final PaymentOrchestratorService paymentService;
    public PaymentApiController(TransactionRepository txRepo, TxEventRepository eventRepo, ExternalPgClient externalPgClient, PaymentOrchestratorService paymentService) {
        this.txRepo = txRepo;
        this.eventRepo = eventRepo;
        this.externalPgClient = externalPgClient;
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizeRequest req) {
        // 1) 멱등키로 기존 거래 있으면 그대로 반환 (DB기반 1차 멱등성)
        var existing = txRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return ResponseEntity.ok(AuthorizeResponse.from(existing.get()));
        }

        Transaction tx = Transaction.create(
                req.merchantId(),
                req.amount(),
                req.currency(),
                idempotencyKey
        );

        tx = txRepo.save(tx);
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CREATED, "Transaction created"));

        // 1. ROUTED
        tx.routedTo("PG-MOCK");
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.ROUTED_TO_EXTERNAL, "Routed to PG-MOCK"));

        // 2. PG 호출
        var pgResponse = externalPgClient.authorize(
                new ExternalPgClient.PgRequest(
                        tx.getMerchantId(),
                        tx.getAmount(),
                        tx.getCurrency()
                )
        );

        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.EXTERNAL_RESPONSE,
                "PG resultCode=" + pgResponse.resultCode()));

        // 3. 결과 반영
        if ("00".equals(pgResponse.resultCode())) {
            tx.authorized(pgResponse.resultCode(), pgResponse.approvalNo());
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.AUTHORIZED, "Approved"));
        } else {
            tx.failed(pgResponse.resultCode(), pgResponse.message());
            eventRepo.save(TxEvent.of(tx.getId(), TxEventType.FAILED, "Declined"));
        }

        txRepo.save(tx);

        return ResponseEntity.ok(AuthorizeResponse.from(tx));
    }

    @GetMapping("/{txId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(@PathVariable UUID txId) {

        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        var events = eventRepo.findByTxIdOrderByCreatedAtAsc(txId);

        return ResponseEntity.ok(PaymentDetailResponse.from(tx, events));
    }


    @PostMapping("/{txId}/cancel")
    public ResponseEntity<CancelResponse> cancel(
            @PathVariable UUID txId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        CancelResponse res = paymentService.cancel(txId, idempotencyKey);
        return ResponseEntity.ok(res);
    }

    public record AuthorizeRequest(
            @NotBlank String merchantId,
            @Min(1) long amount,
            @NotBlank String currency
    ) {}

    public record AuthorizeResponse(
            UUID txId,
            String status,
            String merchantId,
            long amount,
            String currency
    ) {
        static AuthorizeResponse from(Transaction tx) {
            return new AuthorizeResponse(
                    tx.getId(),
                    tx.getStatus().name(),
                    tx.getMerchantId(),
                    tx.getAmount(),
                    tx.getCurrency()
            );
        }
    }

    public record PaymentDetailResponse(
            UUID txId,
            String status,
            String merchantId,
            long amount,
            String currency,
            List<EventResponse> events
    ) {
        static PaymentDetailResponse from(Transaction tx, List<TxEvent> events) {
            return new PaymentDetailResponse(
                    tx.getId(),
                    tx.getStatus().name(),
                    tx.getMerchantId(),
                    tx.getAmount(),
                    tx.getCurrency(),
                    events.stream()
                            .map(e -> new EventResponse(
                                    e.getEventType().name(),
                                    e.getMessage(),
                                    e.getCreatedAt()
                            ))
                            .toList()
            );
        }
    }



    public record EventResponse(
            String eventType,
            String message,
            Instant createdAt
    ) {}

}


