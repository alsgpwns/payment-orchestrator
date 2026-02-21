package com.hjmin.payment.pgmock.controller;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pg")
public class PgMockController {

    @PostMapping("/authorize")
    public PgResponse authorize(@RequestBody PgRequest req) {

        long amount = req.amount();

        if (amount < 1000) {
            return new PgResponse("13", null, "Invalid amount (min=1000)");
        }
        String approvalNo = "A" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PgResponse("00", approvalNo, "Approved");

    }

    @PostMapping("/cancel")
    public PgCancelResponse cancel(@RequestBody PgCancelRequest req) {
        // 테스트 룰: 10만원 초과면 거절
        boolean approved = req.amount() <= 100_000;

        if (approved) {
            return new PgCancelResponse(true, "CANCEL_OK", "canceled");
        }
        return new PgCancelResponse(false, "CANCEL_DENIED", "deny by rule");
    }

    public record PgRequest(String merchantId, long amount, String currency) {}
    public record PgResponse(String resultCode, String approvalNo, String message) {}

    public record PgCancelRequest(UUID txId, String merchantId, Long amount, String currency) {}
    public record PgCancelResponse(boolean approved, String resultCode, String message) {}

}
