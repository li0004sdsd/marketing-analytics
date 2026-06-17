package com.analytics.service;

import com.analytics.dto.BehaviorEventRequest;
import com.analytics.entity.BehaviorEvent;
import com.analytics.repository.BehaviorEventRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BehaviorEventService {
    private final BehaviorEventRepository behaviorEventRepository;

    public BehaviorEventService(BehaviorEventRepository behaviorEventRepository) {
        this.behaviorEventRepository = behaviorEventRepository;
    }

    public List<BehaviorEvent> getAll() {
        return behaviorEventRepository.findAll();
    }

    public BehaviorEvent track(BehaviorEventRequest request) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(request.getUserId());
        event.setEventName(request.getEventName());
        event.setPage(request.getPage());
        event.setSessionId(request.getSessionId());
        event.setProperties(request.getProperties());
        event.setTimestamp(LocalDateTime.now());
        return behaviorEventRepository.save(event);
    }

    public List<BehaviorEvent> getByUser(String userId) {
        return behaviorEventRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<BehaviorEvent> getByEventName(String eventName) {
        return behaviorEventRepository.findByEventNameOrderByTimestampDesc(eventName);
    }

    public void delete(Long id) {
        behaviorEventRepository.deleteById(id);
    }
}
