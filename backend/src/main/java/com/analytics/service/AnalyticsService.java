package com.analytics.service;

import com.analytics.dto.AnalyticsResponse;
import com.analytics.dto.TrendPoint;
import com.analytics.repository.BehaviorEventRepository;
import com.analytics.repository.EventTypeRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalyticsService {
    private final BehaviorEventRepository behaviorEventRepository;
    private final EventTypeRepository eventTypeRepository;

    public AnalyticsService(BehaviorEventRepository behaviorEventRepository,
                            EventTypeRepository eventTypeRepository) {
        this.behaviorEventRepository = behaviorEventRepository;
        this.eventTypeRepository = eventTypeRepository;
    }

    public AnalyticsResponse getSummary() {
        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalEvents(behaviorEventRepository.count());
        response.setTotalUsers(behaviorEventRepository.countDistinctUsers());
        response.setTotalEventTypes(eventTypeRepository.count());

        List<Object[]> eventCounts = behaviorEventRepository.countByEventName();
        List<Map<String, Object>> eventDist = new ArrayList<>();
        for (Object[] row : eventCounts) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", row[0]);
            item.put("count", row[1]);
            eventDist.add(item);
        }
        response.setEventDistribution(eventDist);

        List<Object[]> pageCounts = behaviorEventRepository.countByPage();
        List<Map<String, Object>> pageDist = new ArrayList<>();
        for (Object[] row : pageCounts) {
            Map<String, Object> item = new HashMap<>();
            item.put("page", row[0]);
            item.put("count", row[1]);
            pageDist.add(item);
        }
        response.setPageDistribution(pageDist);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyCounts = behaviorEventRepository.countByDay(thirtyDaysAgo);
        List<TrendPoint> trend = new ArrayList<>();
        for (Object[] row : dailyCounts) {
            trend.add(new TrendPoint((String) row[0], (Long) row[1]));
        }
        response.setDailyTrend(trend);

        return response;
    }
}
