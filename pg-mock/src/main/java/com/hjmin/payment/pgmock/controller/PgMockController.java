package com.hjmin.payment.pgmock.controller;

import org.springframework.web.bind.annotation.*;

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

    public record PgRequest(String merchantId, long amount, String currency) {}
    public record PgResponse(String resultCode, String approvalNo, String message) {}
}
