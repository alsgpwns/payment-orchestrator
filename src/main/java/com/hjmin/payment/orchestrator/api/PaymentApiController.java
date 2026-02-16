package com.hjmin.payment.orchestrator.api;

import com.hjmin.payment.orchestrator.domain.Transaction;
import com.hjmin.payment.orchestrator.domain.TxEvent;
import com.hjmin.payment.orchestrator.domain.TxEventType;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApiController {

    private final TransactionRepository txRepo;
    private final TxEventRepository eventRepo;

    public PaymentApiController(TransactionRepository txRepo, TxEventRepository eventRepo) {
        this.txRepo = txRepo;
        this.eventRepo = eventRepo;
    }

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizeRequest req) {
        // 1) 멱등키로 기존 거래 있으면 그대로 반환 (DB기반 1차 멱등성)
        var existing = txRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            return ResponseEntity.ok(AuthorizeResponse.from(tx));
        }

        // 2) 신규 거래 생성 + 이벤트 기록
        Transaction tx = Transaction.create(req.merchantId(), req.amount(), req.currency(), idempotencyKey);
        tx = txRepo.save(tx);
        eventRepo.save(TxEvent.of(tx.getId(), TxEventType.CREATED, "Transaction created"));

        // 3) 일단 MVP는 라우팅/승인 모의 없이 CREATED 상태로 반환해도 됨
        return ResponseEntity.ok(AuthorizeResponse.from(tx));
    }

    @GetMapping("/{txId}")
    public ResponseEntity<PaymentDetailResponse> getPayment(@PathVariable UUID txId) {

        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        var events = eventRepo.findByTxIdOrderByCreatedAtAsc(txId);

        return ResponseEntity.ok(PaymentDetailResponse.from(tx, events));
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


