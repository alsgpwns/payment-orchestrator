package com.hjmin.payment.pgmock.controller;

import com.hjmin.payment.pgmock.store.PgTxStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pg")
public class PgMockController {

    private final PgTxStore store;

    public PgMockController(PgTxStore store) {
        this.store = store;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }


    @PostMapping("/authorize")
    public PgResponse authorize(@RequestBody PgRequest req) {

        if (req.txId() == null) {
            return new PgResponse("99", null, "txId is required");
        }

        long amount = req.amount();

        if (amount < 1000) {
            store.upsert(new PgTxStore.PgTxSnapshot(
                    req.txId(),
                    PgTxStore.PgTxState.FAILED,
                    "13",
                    "Invalid amount (min=1000)"
            ));
            return new PgResponse("13", null, "Invalid amount (min=1000)");
        }

        String approvalNo = "A" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        store.upsert(new PgTxStore.PgTxSnapshot(
                req.txId(),
                PgTxStore.PgTxState.AUTHORIZED,
                "00",
                "Approved"
        ));

        return new PgResponse("00", approvalNo, "Approved");
    }


    @PostMapping("/cancel")
    public PgCancelResponse cancel(@RequestBody PgCancelRequest req) {

        if (req.txId() == null) {
            return new PgCancelResponse(false, "99",  "txId is required");
        }

        // 테스트 룰: 10만원 초과면 거절
        boolean ok = req.amount() <= 100_000;

        if (ok) {
            store.upsert(new PgTxStore.PgTxSnapshot(
                    req.txId(),
                    PgTxStore.PgTxState.CANCELED,
                    "CANCEL_OK",
                    "canceled"
            ));
            return new PgCancelResponse(true, "CANCEL_OK", "canceled");
        }

        store.upsert(new PgTxStore.PgTxSnapshot(
                req.txId(),
                PgTxStore.PgTxState.CANCEL_FAILED,
                "CANCEL_DENIED",
                "deny by rule"
        ));
        return new PgCancelResponse(false, "CANCEL_DENIED", "deny by rule");
    }

    @GetMapping("/tx/{txId}")
    public ResponseEntity<PgTxStatusResponse> getTx(@PathVariable UUID txId) {
        PgTxStore.PgTxSnapshot snap = store.get(txId);
        if (snap == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new PgTxStatusResponse(
                snap.txId(),
                snap.state().name(),
                snap.resultCode(),
                snap.message()
        ));
    }

    public record PgTxStatusResponse(
            UUID txId,
            String state,
            String resultCode,
            String message
    ) {}

    public record PgRequest(UUID txId, String merchantId, long amount, String currency) {}
    public record PgResponse(String resultCode, String approvalNo, String message) {}

    public record PgCancelRequest(UUID txId, String merchantId, long amount, String currency) {}
    public record PgCancelResponse(boolean approved, String resultCode, String message) {}

}
