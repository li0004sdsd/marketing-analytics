package com.analytics.repository;

import com.analytics.entity.EventOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {

    List<EventOutbox> findByStatusOrderByCreatedAtAsc(EventOutbox.OutboxStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :status, e.processedAt = :processedAt WHERE e.id = :id")
    void updateStatus(@Param("id") Long id,
                      @Param("status") EventOutbox.OutboxStatus status,
                      @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = :status, e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage WHERE e.id = :id")
    void markFailed(@Param("id") Long id,
                    @Param("status") EventOutbox.OutboxStatus status,
                    @Param("errorMessage") String errorMessage);
}
