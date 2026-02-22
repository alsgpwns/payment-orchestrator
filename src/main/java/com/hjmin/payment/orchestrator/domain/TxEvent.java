package com.hjmin.payment.orchestrator.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tx_events", indexes = {
        @Index(name = "idx_event_tx_id", columnList = "txId"),
        @Index(name = "idx_event_created_at", columnList = "createdAt")
})
public class TxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID txId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TxEventType eventType;

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected TxEvent() {}

    public static TxEvent of(UUID txId, TxEventType type, String message) {
        TxEvent e = new TxEvent();
        e.txId = txId;
        e.eventType = type;
        e.message = message;
        e.createdAt = Instant.now();
        return e;
    }
    public Long getId() { return id; }
    public UUID getTxId() { return txId; }
    public TxEventType getEventType() { return eventType; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
