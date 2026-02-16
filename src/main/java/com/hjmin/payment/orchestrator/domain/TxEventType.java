package com.hjmin.payment.orchestrator.domain;

public enum TxEventType {
    CREATED,
    ROUTED_TO_PG,
    AUTHORIZED,
    FAILED
}
