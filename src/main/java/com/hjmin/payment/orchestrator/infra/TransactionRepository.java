package com.hjmin.payment.orchestrator.infra;

import com.hjmin.payment.orchestrator.domain.Transaction;
import com.hjmin.payment.orchestrator.domain.TxStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
select t from Transaction t
where t.createdAt >= :from
and t.status in :statuses
order by t.createdAt desc
""")
    List<Transaction> findRecentTargets(@Param("from") Instant from,
                                        @Param("statuses") java.util.Set<TxStatus> statuses);
}
