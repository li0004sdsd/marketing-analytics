package com.analytics.service;

import com.analytics.dto.FunnelAnalysisResponse;
import com.analytics.dto.StepConversion;
import com.analytics.entity.Funnel;
import com.analytics.entity.FunnelStep;
import com.analytics.repository.BehaviorEventRepository;
import com.analytics.repository.FunnelRepository;
import com.analytics.repository.FunnelStepRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FunnelAnalysisService {
    private final FunnelRepository funnelRepository;
    private final FunnelStepRepository funnelStepRepository;
    private final BehaviorEventRepository behaviorEventRepository;

    public FunnelAnalysisService(FunnelRepository funnelRepository,
                                 FunnelStepRepository funnelStepRepository,
                                 BehaviorEventRepository behaviorEventRepository) {
        this.funnelRepository = funnelRepository;
        this.funnelStepRepository = funnelStepRepository;
        this.behaviorEventRepository = behaviorEventRepository;
    }

    public FunnelAnalysisResponse getStepConversion(Long funnelId) {
        Funnel funnel = funnelRepository.findById(funnelId)
                .orElseThrow(() -> new RuntimeException("Funnel not found"));

        List<FunnelStep> steps = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        while (true) {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<FunnelStep> stepPage = funnelStepRepository.findByFunnelId(funnelId, pageable);
            steps.addAll(stepPage.getContent());
            if (stepPage.isLast()) {
                break;
            }
            page++;
        }

        List<StepConversion> conversions = new ArrayList<>();
        Long previousCount = null;
        Long firstCount = null;

        for (FunnelStep step : steps) {
            Long count = countUsersForEvent(step.getEventName());

            Double conversionRate = null;
            Double dropOffRate = null;

            if (previousCount != null && previousCount > 0) {
                conversionRate = (double) count / previousCount * 100;
                dropOffRate = 100 - conversionRate;
            } else if (previousCount == null) {
                conversionRate = 100.0;
                dropOffRate = 0.0;
                firstCount = count;
            }

            conversions.add(new StepConversion(
                    step.getStepOrder(),
                    step.getStepName(),
                    count,
                    conversionRate,
                    dropOffRate
            ));

            previousCount = count;
        }

        FunnelAnalysisResponse response = new FunnelAnalysisResponse();
        response.setFunnelId(funnel.getId());
        response.setFunnelName(funnel.getName());
        response.setStepConversions(conversions);
        response.setTotalUsers(firstCount);

        if (firstCount != null && firstCount > 0 && !conversions.isEmpty()) {
            Long lastCount = conversions.get(conversions.size() - 1).getUserCount();
            response.setOverallConversionRate((double) lastCount / firstCount * 100);
        }

        return response;
    }

    private Long countUsersForEvent(String eventName) {
        List<String> userIds = behaviorEventRepository.findByEventNameOrderByTimestampDesc(eventName)
                .stream()
                .map(e -> e.getUserId())
                .distinct()
                .collect(Collectors.toList());
        return (long) userIds.size();
    }
}
