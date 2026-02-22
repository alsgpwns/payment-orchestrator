package com.hjmin.payment.pgmock.store;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PgTxStore {

    public enum PgTxState {
        AUTHORIZED,
        FAILED,
        CANCELED,
        CANCEL_FAILED
    }

    public record PgTxSnapshot(
            UUID txId,
            PgTxState state,
            String resultCode,
            String message
    ) {}

    private final Map<UUID, PgTxSnapshot> store = new ConcurrentHashMap<>();

    public void upsert(PgTxSnapshot snapshot) {
        store.put(snapshot.txId(), snapshot);
    }

    public PgTxSnapshot get(UUID txId) {
        return store.get(txId);
    }
}