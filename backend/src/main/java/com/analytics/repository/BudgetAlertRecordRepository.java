package com.analytics.repository;

import com.analytics.entity.BudgetAlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetAlertRecordRepository extends JpaRepository<BudgetAlertRecord, Long> {

    boolean existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
            Long campaignId,
            LocalDate alertDate,
            BigDecimal alertThresholdPercent
    );

    List<BudgetAlertRecord> findByCampaignIdOrderByAlertDateDesc(Long campaignId);

    List<BudgetAlertRecord> findByAlertDate(LocalDate alertDate);

    long countByCampaignIdAndAlertDate(Long campaignId, LocalDate alertDate);
}
