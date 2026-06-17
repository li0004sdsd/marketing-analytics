package com.analytics.repository;

import com.analytics.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    List<EventType> findByActiveTrue();
    Optional<EventType> findByName(String name);
    boolean existsByName(String name);
}
