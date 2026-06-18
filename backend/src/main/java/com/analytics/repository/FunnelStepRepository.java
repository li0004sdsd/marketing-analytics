package com.analytics.repository;

import com.analytics.entity.FunnelStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FunnelStepRepository extends JpaRepository<FunnelStep, Long> {

    @Query("SELECT fs FROM FunnelStep fs WHERE fs.funnel.id = :funnelId ORDER BY fs.stepOrder ASC")
    Page<FunnelStep> findByFunnelId(@Param("funnelId") Long funnelId, Pageable pageable);
}
