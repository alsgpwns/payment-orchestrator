package com.hjmin.payment.orchestrator.infra.external.dto;

public record PgCancelResponse(
        boolean approved,
        String resultCode,
        String message
) {}