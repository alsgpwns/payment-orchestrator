package com.hjmin.payment.orchestrator.domain;

public enum TxStatus {
    CREATED,
    ROUTED_TO_EXTERNAL,
    EXTERNAL_RESPONSE,
    AUTHORIZED,
    FAILED
}
