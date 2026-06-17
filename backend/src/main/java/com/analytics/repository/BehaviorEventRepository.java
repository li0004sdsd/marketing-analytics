package com.analytics.repository;

import com.analytics.entity.BehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BehaviorEventRepository extends JpaRepository<BehaviorEvent, Long> {
    List<BehaviorEvent> findByUserIdOrderByTimestampDesc(String userId);
    List<BehaviorEvent> findByEventNameOrderByTimestampDesc(String eventName);
    List<BehaviorEvent> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b.eventName, COUNT(b) FROM BehaviorEvent b GROUP BY b.eventName ORDER BY COUNT(b) DESC")
    List<Object[]> countByEventName();

    @Query("SELECT b.page, COUNT(b) FROM BehaviorEvent b WHERE b.page IS NOT NULL GROUP BY b.page ORDER BY COUNT(b) DESC")
    List<Object[]> countByPage();

    @Query("SELECT FUNCTION('FORMATDATETIME', b.timestamp, 'yyyy-MM-dd'), COUNT(b) FROM BehaviorEvent b WHERE b.timestamp >= :start GROUP BY FUNCTION('FORMATDATETIME', b.timestamp, 'yyyy-MM-dd') ORDER BY FUNCTION('FORMATDATETIME', b.timestamp, 'yyyy-MM-dd')")
    List<Object[]> countByDay(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(DISTINCT b.userId) FROM BehaviorEvent b")
    Long countDistinctUsers();
}
