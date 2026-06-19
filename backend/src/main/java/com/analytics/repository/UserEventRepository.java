package com.analytics.repository;

import com.analytics.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    @Query("SELECT COUNT(u) FROM UserEvent u WHERE u.userId = :userId AND u.eventTime BETWEEN :start AND :end")
    Long countByUserIdAndEventTimeBetween(@Param("userId") String userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    Long countByUserId(String userId);
}
