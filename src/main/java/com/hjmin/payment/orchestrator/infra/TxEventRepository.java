package com.hjmin.payment.orchestrator.infra;

import com.hjmin.payment.orchestrator.domain.TxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TxEventRepository extends JpaRepository<TxEvent, Long> {
    List<TxEvent> findByTxIdOrderByCreatedAtAsc(UUID txId);
}
