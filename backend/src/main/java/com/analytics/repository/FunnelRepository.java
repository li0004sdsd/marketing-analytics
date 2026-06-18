package com.analytics.repository;

import com.analytics.entity.Funnel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunnelRepository extends JpaRepository<Funnel, Long> {
}
