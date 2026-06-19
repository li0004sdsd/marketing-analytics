package com.analytics.repository;

import com.analytics.entity.BudgetAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BudgetAlertRepository extends JpaRepository<BudgetAlertConfig, Long> {

    Optional<BudgetAlertConfig> findByCampaignId(Long campaignId);

    List<BudgetAlertConfig> findByEnabledTrue();

    @Query("SELECT b FROM BudgetAlertConfig b WHERE b.enabled = true " +
           "AND b.campaignId IN :campaignIds")
    List<BudgetAlertConfig> findEnabledByCampaignIdIn(List<Long> campaignIds);

    boolean existsByCampaignId(Long campaignId);

    void deleteByCampaignId(Long campaignId);
}
