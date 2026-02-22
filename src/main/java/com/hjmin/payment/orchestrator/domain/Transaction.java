package com.hjmin.payment.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_tx_created_at", columnList = "createdAt")
})
public class Transaction {

    @Getter
    @Id
    @GeneratedValue
    private UUID id;

    @Getter
    @Column(nullable = false, length = 50)
    private String merchantId;

    @Getter
    @Column(nullable = false)
    private long amount;

    @Getter
    @Column(nullable = false, length = 3)
    private String currency;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TxStatus status;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Getter
    @Column(length = 10)
    private String selectedPg;

    @Getter
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(nullable = false)
    private Instant updatedAt;

    @Getter
    @Column(length = 10)
    private String pgResultCode;

    @Getter
    @Column(length = 50)
    private String approvalNo;

    @Getter
    @Column(length = 500)
    private String failReason;

    // cancel fields
    @Getter
    @Column(name = "cancel_idempotency_key")
    private String cancelIdempotencyKey;

    @Getter
    @Column(name = "cancel_result_code")
    private String cancelResultCode;

    @Getter
    @Column(name = "cancel_reason")
    private String cancelReason;

    @Getter
    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Getter
    private Instant routedAt;
    @Getter
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


    public void assertCancelable() {
        if (this.status != TxStatus.AUTHORIZED
                && this.status != TxStatus.CANCEL_REQUESTED
                && this.status != TxStatus.CANCELED
                && this.status != TxStatus.CANCEL_FAILED) {
            throw new IllegalStateException("Not cancelable. current=" + this.status);
        }
    }

    public boolean isCancelTerminal() {
        return this.status == TxStatus.CANCELED || this.status == TxStatus.CANCEL_FAILED;
    }

    public void requestCancel(String idempotencyKey) {
        // 최초 cancel 요청만 키를 박아둠
        if (this.cancelIdempotencyKey == null) {
            this.cancelIdempotencyKey = idempotencyKey;
        }
        this.status = TxStatus.CANCEL_REQUESTED;
    }

    public void canceled(String cancelResultCode, String cancelReason) {
        this.status = TxStatus.CANCELED;
        this.cancelResultCode = cancelResultCode;
        this.cancelReason = cancelReason;
        this.canceledAt = Instant.now();
    }

    public void cancelFailed(String cancelResultCode, String cancelReason) {
        this.status = TxStatus.CANCEL_FAILED;
        this.cancelResultCode = cancelResultCode;
        this.cancelReason = cancelReason;
    }

    public void verifyCancelIdempotencyKey(String incomingKey) {
        // 이미 cancelIdempotencyKey가 있는데 다른 키로 “진행 중 cancel”을 또 실행시 정책적으로 막는 게 안전
        if (this.status == TxStatus.CANCEL_REQUESTED
                && this.cancelIdempotencyKey != null
                && !this.cancelIdempotencyKey.equals(incomingKey)) {
            throw new IllegalStateException("Cancel already in progress with different Idempotency-Key");
        }
    }


}
