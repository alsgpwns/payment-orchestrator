package com.hjmin.payment.orchestrator.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_tx_created_at", columnList = "createdAt")
})
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String merchantId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TxStatus status;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(length = 10)
    private String selectedPg;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 10)
    private String pgResultCode;

    @Column(length = 50)
    private String approvalNo;

    @Column(length = 500)
    private String failReason;

    private Instant routedAt;
    private Instant authorizedAt;


    protected Transaction() {}

    public static Transaction create(String merchantId, long amount, String currency, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.merchantId = merchantId;
        tx.amount = amount;
        tx.currency = currency;
        tx.idempotencyKey = idempotencyKey;
        tx.status = TxStatus.CREATED;
        tx.createdAt = Instant.now();
        tx.updatedAt = tx.createdAt;
        return tx;
    }

    public UUID getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TxStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSelectedPg() { return selectedPg; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getPgResultCode() { return pgResultCode; }
    public String getApprovalNo() { return approvalNo; }
    public String getFailReason() { return failReason; }
    public Instant getRoutedAt() { return routedAt; }
    public Instant getAuthorizedAt() { return authorizedAt; }


    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void routedTo(String pg) {
        this.selectedPg = pg;
        this.status = TxStatus.ROUTED_TO_EXTERNAL;
        this.routedAt = Instant.now();
    }
    public void applyExternalResponse(String pgResultCode, String approvalNo) {
        this.status = TxStatus.EXTERNAL_RESPONSE;
        this.pgResultCode = pgResultCode;
        this.approvalNo = approvalNo;
    }

    public void authorized() {
        this.status = TxStatus.AUTHORIZED;
    }

    public void failed() {
        this.status = TxStatus.FAILED;
    }

    public void authorized(String pgResultCode, String approvalNo) {
        this.status = TxStatus.AUTHORIZED;
        this.pgResultCode = pgResultCode;
        this.approvalNo = approvalNo;
        this.failReason = null;
        this.authorizedAt = Instant.now();
    }

    public void failed(String pgResultCode, String failReason) {
        this.status = TxStatus.FAILED;
        this.pgResultCode = pgResultCode;
        this.failReason = failReason;
        this.approvalNo = null;
    }



}
