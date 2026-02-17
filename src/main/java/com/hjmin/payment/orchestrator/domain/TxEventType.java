package com.hjmin.payment.orchestrator.domain;

public enum TxEventType {
    CREATED,
    ROUTED_TO_EXTERNAL,
    EXTERNAL_RESPONSE,
    AUTHORIZED,
    FAILED
}
