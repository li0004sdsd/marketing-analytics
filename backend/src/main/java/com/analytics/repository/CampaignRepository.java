package com.analytics.repository;

import com.analytics.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByTeamId(Long teamId);
}
