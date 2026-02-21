package com.hjmin.payment.orchestrator.api;

import java.util.UUID;

public record CancelResponse(
        UUID txId,
        String status,
        String cancelResultCode,
        String cancelReason
) {}
