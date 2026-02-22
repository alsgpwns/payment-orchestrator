package com.hjmin.payment.orchestrator.infra;

import com.hjmin.payment.orchestrator.domain.TxEvent;
import com.hjmin.payment.orchestrator.domain.TxEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TxEventRepository extends JpaRepository<TxEvent, Long> {
    List<TxEvent> findByTxIdOrderByCreatedAtAsc(UUID txId);
    List<TxEvent> findAllByTxIdOrderByCreatedAtAsc(UUID txId);
    Optional<TxEvent> findTopByTxIdAndEventTypeInOrderByCreatedAtDesc(UUID txId, Collection<TxEventType> types);
}
