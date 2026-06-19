package com.analytics.repository;

import com.analytics.entity.CampaignTouchPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TouchPointRepository extends JpaRepository<CampaignTouchPoint, Long> {

    List<CampaignTouchPoint> findByCampaignId(Long campaignId);

    List<CampaignTouchPoint> findByCampaignIdIn(Collection<Long> campaignIds);

    @Query("SELECT c FROM CampaignTouchPoint c WHERE c.campaignId IN :campaignIds " +
           "AND c.touchTime BETWEEN :start AND :end ORDER BY c.campaignId, c.touchTime ASC")
    List<CampaignTouchPoint> findByCampaignIdInAndTouchTimeBetween(
            @Param("campaignIds") Collection<Long> campaignIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(c) FROM CampaignTouchPoint c WHERE c.campaignId = :campaignId")
    Long countByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COALESCE(SUM(c.revenue), 0) FROM CampaignTouchPoint c WHERE c.campaignId = :campaignId")
    java.math.BigDecimal sumRevenueByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT c.campaignId, COALESCE(SUM(c.revenue), 0) FROM CampaignTouchPoint c " +
           "WHERE c.campaignId IN :campaignIds GROUP BY c.campaignId")
    List<Object[]> sumRevenueByCampaignIdIn(@Param("campaignIds") Collection<Long> campaignIds);
}
