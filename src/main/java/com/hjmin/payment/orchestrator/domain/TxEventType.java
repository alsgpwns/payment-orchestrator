package com.hjmin.payment.orchestrator.domain;

public enum TxEventType {
    CREATED,
    ROUTED_TO_EXTERNAL,
    EXTERNAL_RESPONSE,
    AUTHORIZED,
    FAILED,

    CANCEL_REQUESTED,
    ROUTED_CANCEL_TO_EXTERNAL,
    EXTERNAL_CANCEL_RESPONSE,
    CANCELED,
    CANCEL_FAILED
}
