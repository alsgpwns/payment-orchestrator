package com.hjmin.payment.orchestrator.infra.external.dto;

import java.util.UUID;

public record PgCancelRequest(
        UUID txId,
        String merchantId,
        Long amount,
        String currency
) {}
