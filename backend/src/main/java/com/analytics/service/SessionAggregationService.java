package com.analytics.service;

import com.analytics.entity.Session;
import com.analytics.entity.UserEvent;
import com.analytics.repository.SessionRepository;
import com.analytics.repository.UserEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SessionAggregationService {

    private static final Logger log = LoggerFactory.getLogger(SessionAggregationService.class);

    private static final Set<String> PAGE_VIEW_EVENTS = Set.of(
            "page_view", "pageview", "view_page", "page_load"
    );

    private static final Set<String> CONVERSION_EVENTS = Set.of(
            "purchase", "checkout", "signup", "sign_up", "register",
            "conversion", "convert", "submit_order", "place_order",
            "order_complete", "purchase_complete"
    );

    private final UserEventRepository userEventRepository;
    private final SessionRepository sessionRepository;

    public SessionAggregationService(UserEventRepository userEventRepository,
                                     SessionRepository sessionRepository) {
        this.userEventRepository = userEventRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public int aggregate() {
        Set<String> sessionIds = userEventRepository.findDistinctSessionIds();
        int processed = 0;

        for (String sessionId : sessionIds) {
            try {
                List<UserEvent> events = userEventRepository.findBySessionIdOrderByEventTimeAsc(sessionId);
                if (events == null || events.isEmpty()) {
                    continue;
                }

                Session aggregated = buildSessionFromEvents(sessionId, events);
                upsertSession(aggregated);
                processed++;
            } catch (Exception e) {
                log.error("Failed to aggregate session: {}", sessionId, e);
            }
        }

        log.info("Session aggregation completed. Processed {} sessions.", processed);
        return processed;
    }

    @Transactional
    public int aggregate(LocalDateTime start, LocalDateTime end) {
        Set<String> sessionIds = userEventRepository.findDistinctSessionIdsByEventTimeBetween(start, end);
        int processed = 0;

        for (String sessionId : sessionIds) {
            try {
                List<UserEvent> events = userEventRepository.findBySessionIdOrderByEventTimeAsc(sessionId);
                if (events == null || events.isEmpty()) {
                    continue;
                }

                Session aggregated = buildSessionFromEvents(sessionId, events);
                upsertSession(aggregated);
                processed++;
            } catch (Exception e) {
                log.error("Failed to aggregate session: {}", sessionId, e);
            }
        }

        log.info("Session aggregation completed for time range. Processed {} sessions.", processed);
        return processed;
    }

    Session buildSessionFromEvents(String sessionId, List<UserEvent> events) {
        UserEvent first = events.get(0);
        UserEvent last = events.get(events.size() - 1);

        Session session = new Session();
        session.setSessionId(sessionId);
        session.setUserId(first.getUserId());
        session.setStartTime(first.getEventTime());
        session.setEndTime(last.getEventTime());
        session.setDuration(calculateDuration(first.getEventTime(), last.getEventTime()));
        session.setPageViews(countPageViews(events));
        session.setConversionFlag(hasConversion(events));
        session.setUpdatedAt(LocalDateTime.now());

        return session;
    }

    long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(start, end).getSeconds());
    }

    int countPageViews(List<UserEvent> events) {
        if (events == null) {
            return 0;
        }
        int count = 0;
        for (UserEvent event : events) {
            if (isPageViewEvent(event.getEventName())) {
                count++;
            }
        }
        return count;
    }

    boolean hasConversion(List<UserEvent> events) {
        if (events == null) {
            return false;
        }
        for (UserEvent event : events) {
            if (isConversionEvent(event.getEventName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPageViewEvent(String eventName) {
        if (eventName == null) {
            return false;
        }
        String normalized = eventName.trim().toLowerCase();
        return PAGE_VIEW_EVENTS.contains(normalized)
                || normalized.contains("page_view")
                || normalized.contains("pageview");
    }

    private boolean isConversionEvent(String eventName) {
        if (eventName == null) {
            return false;
        }
        String normalized = eventName.trim().toLowerCase();
        return CONVERSION_EVENTS.contains(normalized)
                || normalized.contains("conversion")
                || normalized.contains("purchase")
                || normalized.contains("checkout")
                || normalized.contains("signup")
                || normalized.contains("sign_up")
                || normalized.contains("place_order")
                || normalized.contains("order_complete");
    }

    private void upsertSession(Session aggregated) {
        Optional<Session> existing = sessionRepository.findBySessionId(aggregated.getSessionId());

        if (existing.isPresent()) {
            Session toUpdate = existing.get();
            toUpdate.setUserId(aggregated.getUserId());
            toUpdate.setStartTime(aggregated.getStartTime());
            toUpdate.setEndTime(aggregated.getEndTime());
            toUpdate.setDuration(aggregated.getDuration());
            toUpdate.setPageViews(aggregated.getPageViews());
            toUpdate.setConversionFlag(aggregated.getConversionFlag());
            toUpdate.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(toUpdate);
        } else {
            aggregated.setCreatedAt(LocalDateTime.now());
            sessionRepository.save(aggregated);
        }
    }

    public Optional<Session> getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }
}
