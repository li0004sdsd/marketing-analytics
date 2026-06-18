package com.analytics.repository;

import com.analytics.entity.AttributionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttributionConfigRepository extends JpaRepository<AttributionConfig, Long> {
    Optional<AttributionConfig> findByName(String name);
}
