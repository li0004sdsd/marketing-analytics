package com.analytics.repository;

import com.analytics.entity.RoiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoiReportRepository extends JpaRepository<RoiReport, Long>, JpaSpecificationExecutor<RoiReport> {
}
