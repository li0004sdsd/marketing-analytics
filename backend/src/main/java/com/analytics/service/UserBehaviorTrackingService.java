package com.analytics.service;

import com.analytics.entity.EventOutbox;
import com.analytics.entity.UserEvent;
import com.analytics.repository.EventOutboxRepository;
import com.analytics.repository.UserEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserBehaviorTrackingService {
    private static final long IDEMPOTENT_WINDOW_SECONDS = 5;
    private static final int BATCH_SIZE = 100;

    private final UserEventRepository userEventRepository;
    private final EventOutboxRepository eventOutboxRepository;
    private final ConcurrentHashMap<String, LocalDateTime> idempotentCache = new ConcurrentHashMap<>();

    public UserBehaviorTrackingService(UserEventRepository userEventRepository,
                                       EventOutboxRepository eventOutboxRepository) {
        this.userEventRepository = userEventRepository;
        this.eventOutboxRepository = eventOutboxRepository;
    }

    @Transactional
    public void recordEvent(String userId, String eventName, String sessionId, String properties, LocalDateTime eventTime) {
        String idempotentKey = buildIdempotentKey(userId, eventName);

        if (isDuplicate(idempotentKey)) {
            return;
        }

        EventOutbox outbox = new EventOutbox();
        outbox.setUserId(userId);
        outbox.setEventName(eventName);
        outbox.setSessionId(sessionId);
        outbox.setProperties(properties);
        outbox.setEventTime(eventTime != null ? eventTime : LocalDateTime.now());
        outbox.setStatus(EventOutbox.OutboxStatus.PENDING);
        eventOutboxRepository.save(outbox);
    }

    public Long getEventCountByUser(String userId) {
        return userEventRepository.countByUserId(userId);
    }

    public Long getEventCountByUser(String userId, LocalDateTime start, LocalDateTime end) {
        return userEventRepository.countByUserIdAndEventTimeBetween(userId, start, end);
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutboxBatch() {
        List<EventOutbox> pending = eventOutboxRepository.findByStatusOrderByCreatedAtAsc(
                EventOutbox.OutboxStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (pending.isEmpty()) {
            return;
        }

        List<UserEvent> events = new ArrayList<>();
        List<Long> successIds = new ArrayList<>();

        for (EventOutbox outbox : pending) {
            try {
                UserEvent event = new UserEvent();
                event.setUserId(outbox.getUserId());
                event.setEventName(outbox.getEventName());
                event.setSessionId(outbox.getSessionId());
                event.setProperties(outbox.getProperties());
                event.setEventTime(outbox.getEventTime());
                events.add(event);
                successIds.add(outbox.getId());
            } catch (Exception e) {
                eventOutboxRepository.markFailed(
                        outbox.getId(),
                        EventOutbox.OutboxStatus.FAILED,
                        e.getMessage()
                );
            }
        }

        if (!events.isEmpty()) {
            userEventRepository.saveAll(events);
            LocalDateTime now = LocalDateTime.now();
            for (Long id : successIds) {
                eventOutboxRepository.updateStatus(id, EventOutbox.OutboxStatus.SUCCESS, now);
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanIdempotentCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(IDEMPOTENT_WINDOW_SECONDS * 2);
        Iterator<String> iterator = idempotentCache.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            LocalDateTime time = idempotentCache.get(key);
            if (time.isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    private boolean isDuplicate(String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = idempotentCache.get(key);

        if (lastTime != null && !lastTime.plusSeconds(IDEMPOTENT_WINDOW_SECONDS).isBefore(now)) {
            return true;
        }

        idempotentCache.put(key, now);
        return false;
    }

    private String buildIdempotentKey(String userId, String eventName) {
        return userId + ":" + eventName;
    }
}
