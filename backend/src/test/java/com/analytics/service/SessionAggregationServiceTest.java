package com.analytics.service;

import com.analytics.entity.Session;
import com.analytics.entity.UserEvent;
import com.analytics.repository.SessionRepository;
import com.analytics.repository.UserEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAggregationServiceTest {

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private SessionRepository sessionRepository;

    private SessionAggregationService service;

    @BeforeEach
    void setUp() {
        service = new SessionAggregationService(userEventRepository, sessionRepository);
    }

    @Test
    @DisplayName("duration计算：起止时间差秒数，负值应返回0")
    void testCalculateDuration() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 10, 5, 30);
        assertEquals(330L, service.calculateDuration(start, end), "5分30秒应为330秒");

        LocalDateTime same = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        assertEquals(0L, service.calculateDuration(same, same), "同一时间点duration应为0");

        assertEquals(0L, service.calculateDuration(end, start), "end早于start应返回0");
        assertEquals(0L, service.calculateDuration(null, end), "null start应返回0");
        assertEquals(0L, service.calculateDuration(start, null), "null end应返回0");
    }

    @Test
    @DisplayName("page_views计数：匹配page_view类事件名")
    void testCountPageViews() {
        List<UserEvent> events = new ArrayList<>();
        events.add(createEvent("page_view", LocalDateTime.now()));
        events.add(createEvent("click", LocalDateTime.now()));
        events.add(createEvent("PAGE_VIEW", LocalDateTime.now()));
        events.add(createEvent("PageView", LocalDateTime.now()));
        events.add(createEvent("scroll", LocalDateTime.now()));
        events.add(createEvent("page_load", LocalDateTime.now()));

        assertEquals(4, service.countPageViews(events));

        assertEquals(0, service.countPageViews(null));
        assertEquals(0, service.countPageViews(Collections.emptyList()));

        List<UserEvent> nullNameEvents = Collections.singletonList(createEvent(null, LocalDateTime.now()));
        assertEquals(0, service.countPageViews(nullNameEvents));
    }

    @Test
    @DisplayName("conversion_flag判断：匹配转化类事件名")
    void testHasConversion() {
        List<UserEvent> noConversion = Arrays.asList(
                createEvent("page_view", LocalDateTime.now()),
                createEvent("click", LocalDateTime.now())
        );
        assertFalse(service.hasConversion(noConversion), "无转化事件应返回false");

        List<UserEvent> withPurchase = Arrays.asList(
                createEvent("page_view", LocalDateTime.now()),
                createEvent("Purchase", LocalDateTime.now())
        );
        assertTrue(service.hasConversion(withPurchase), "包含Purchase应返回true");

        List<UserEvent> withSignup = Collections.singletonList(createEvent("sign_up", LocalDateTime.now()));
        assertTrue(service.hasConversion(withSignup), "包含sign_up应返回true");

        List<UserEvent> withCheckout = Collections.singletonList(createEvent("checkout_complete", LocalDateTime.now()));
        assertTrue(service.hasConversion(withCheckout), "包含checkout关键词应返回true");

        assertFalse(service.hasConversion(null));
        assertFalse(service.hasConversion(Collections.emptyList()));

        List<UserEvent> nullName = Collections.singletonList(createEvent(null, LocalDateTime.now()));
        assertFalse(service.hasConversion(nullName));
    }

    @Test
    @DisplayName("buildSessionFromEvents：正确组装session各字段")
    void testBuildSessionFromEvents() {
        String sessionId = "sess-001";
        LocalDateTime t1 = LocalDateTime.of(2025, 6, 1, 14, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 14, 3, 0);
        LocalDateTime t3 = LocalDateTime.of(2025, 6, 1, 14, 10, 0);

        List<UserEvent> events = Arrays.asList(
                createEvent("user-001", "page_view", sessionId, t1),
                createEvent("user-001", "click", sessionId, t2),
                createEvent("user-001", "purchase", sessionId, t3)
        );

        Session result = service.buildSessionFromEvents(sessionId, events);

        assertEquals("sess-001", result.getSessionId());
        assertEquals("user-001", result.getUserId());
        assertEquals(t1, result.getStartTime());
        assertEquals(t3, result.getEndTime());
        assertEquals(600L, result.getDuration(), "10分钟 = 600秒");
        assertEquals(1, result.getPageViews());
        assertTrue(result.getConversionFlag());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("aggregate：单事件会话duration为0，正确计数page_views")
    void testAggregate_SingleEventSession() {
        String sessionId = "sess-single";
        LocalDateTime t = LocalDateTime.of(2025, 6, 1, 14, 0, 0);

        Set<String> sessionIds = Set.of(sessionId);
        List<UserEvent> events = Collections.singletonList(
                createEvent("user-001", "page_view", sessionId, t)
        );

        when(userEventRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc(sessionId)).thenReturn(events);
        when(sessionRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        int processed = service.aggregate();

        assertEquals(1, processed);

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());

        Session saved = captor.getValue();
        assertEquals(sessionId, saved.getSessionId());
        assertEquals(0L, saved.getDuration(), "单事件会话duration应为0");
        assertEquals(1, saved.getPageViews());
        assertFalse(saved.getConversionFlag());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("aggregate：多个会话全部正确处理，返回处理数量")
    void testAggregate_MultipleSessions() {
        Set<String> sessionIds = Set.of("sess-001", "sess-002", "sess-003");

        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 10, 0, 0);

        List<UserEvent> events1 = Arrays.asList(
                createEvent("u1", "page_view", "sess-001", base),
                createEvent("u1", "page_view", "sess-001", base.plusMinutes(2)),
                createEvent("u1", "purchase", "sess-001", base.plusMinutes(5))
        );

        List<UserEvent> events2 = Arrays.asList(
                createEvent("u2", "page_view", "sess-002", base),
                createEvent("u2", "click", "sess-002", base.plusMinutes(1))
        );

        List<UserEvent> events3 = Collections.singletonList(
                createEvent("u3", "signup", "sess-003", base.plusHours(1))
        );

        when(userEventRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc("sess-001")).thenReturn(events1);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc("sess-002")).thenReturn(events2);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc("sess-003")).thenReturn(events3);
        when(sessionRepository.findBySessionId(any())).thenReturn(Optional.empty());

        int processed = service.aggregate();
        assertEquals(3, processed);

        verify(sessionRepository, times(3)).save(any(Session.class));

        Map<String, Session> savedSessions = captureSavedSessions(3);

        Session s1 = savedSessions.get("sess-001");
        assertNotNull(s1);
        assertEquals(300L, s1.getDuration());
        assertEquals(2, s1.getPageViews());
        assertTrue(s1.getConversionFlag());

        Session s2 = savedSessions.get("sess-002");
        assertNotNull(s2);
        assertEquals(60L, s2.getDuration());
        assertEquals(1, s2.getPageViews());
        assertFalse(s2.getConversionFlag());

        Session s3 = savedSessions.get("sess-003");
        assertNotNull(s3);
        assertEquals(0L, s3.getDuration());
        assertEquals(0, s3.getPageViews());
        assertTrue(s3.getConversionFlag());
    }

    @Test
    @DisplayName("upsert：session_id已存在时更新而非重复插入，避免脏数据")
    void testAggregate_UpsertExistingSession_UpdatesInsteadOfInsert() {
        String sessionId = "sess-upsert";
        Set<String> sessionIds = Set.of(sessionId);

        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 10, 0, 0);

        List<UserEvent> events = Arrays.asList(
                createEvent("u1", "page_view", sessionId, base),
                createEvent("u1", "page_view", sessionId, base.plusMinutes(3)),
                createEvent("u1", "checkout", sessionId, base.plusMinutes(8))
        );

        Session existingSession = new Session();
        existingSession.setId(999L);
        existingSession.setSessionId(sessionId);
        existingSession.setUserId("u1");
        existingSession.setStartTime(base);
        existingSession.setEndTime(base.plusMinutes(3));
        existingSession.setDuration(180L);
        existingSession.setPageViews(2);
        existingSession.setConversionFlag(false);
        existingSession.setCreatedAt(base.minusDays(1));
        existingSession.setUpdatedAt(base.minusDays(1));

        when(userEventRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc(sessionId)).thenReturn(events);
        when(sessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existingSession));

        int processed = service.aggregate();
        assertEquals(1, processed);

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());

        Session updated = captor.getValue();
        assertEquals(999L, updated.getId(), "应保留原有id，说明是update而非insert");
        assertEquals(sessionId, updated.getSessionId());
        assertEquals(base, updated.getStartTime());
        assertEquals(base.plusMinutes(8), updated.getEndTime());
        assertEquals(480L, updated.getDuration(), "8分钟 = 480秒");
        assertEquals(2, updated.getPageViews());
        assertTrue(updated.getConversionFlag(), "应更新为已转化");
        assertEquals(base.minusDays(1), updated.getCreatedAt(), "createdAt不应被覆盖");
        assertTrue(updated.getUpdatedAt().isAfter(existingSession.getUpdatedAt()),
                "updatedAt应更新为新时间");
    }

    @Test
    @DisplayName("aggregate：重复调用两次不产生重复记录，sessions表中同session_id只存一条")
    void testAggregate_Twice_NoDuplicates() {
        String sessionId = "sess-repeat";
        Set<String> sessionIds = Set.of(sessionId);

        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 10, 0, 0);
        List<UserEvent> events = Arrays.asList(
                createEvent("u1", "page_view", sessionId, base),
                createEvent("u1", "page_view", sessionId, base.plusMinutes(2))
        );

        when(userEventRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc(sessionId)).thenReturn(events);

        Session firstRunExisting = null;
        Session[] existingHolder = {firstRunExisting};

        when(sessionRepository.findBySessionId(sessionId)).thenAnswer(invocation ->
                existingHolder[0] == null ? Optional.empty() : Optional.of(existingHolder[0])
        );

        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            existingHolder[0] = s;
            return s;
        });

        int result1 = service.aggregate();
        assertEquals(1, result1);

        int result2 = service.aggregate();
        assertEquals(1, result2);

        verify(sessionRepository, times(2)).save(any(Session.class));
        verify(sessionRepository, times(2)).findBySessionId(sessionId);

        assertNotNull(existingHolder[0]);
        assertEquals(Long.valueOf(1L), existingHolder[0].getId(),
                "两次保存应共享同一条记录id，没有产生新记录");
    }

    @Test
    @DisplayName("aggregate：无session_id数据或空列表时返回0且不报错")
    void testAggregate_NoData() {
        when(userEventRepository.findDistinctSessionIds()).thenReturn(Collections.emptySet());
        assertEquals(0, service.aggregate());
        verify(sessionRepository, never()).save(any());

        when(userEventRepository.findDistinctSessionIds()).thenReturn(Set.of("sess-empty"));
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc("sess-empty"))
                .thenReturn(Collections.emptyList());
        assertEquals(0, service.aggregate());
    }

    @Test
    @DisplayName("aggregate：某session异常不影响其他session处理")
    void testAggregate_SessionException_DoesNotBreakOthers() {
        String goodSess = "sess-good";
        String badSess = "sess-bad";
        Set<String> sessionIds = Set.of(goodSess, badSess);

        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 10, 0, 0);

        List<UserEvent> goodEvents = Collections.singletonList(
                createEvent("u1", "page_view", goodSess, base)
        );

        when(userEventRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc(goodSess)).thenReturn(goodEvents);
        when(userEventRepository.findBySessionIdOrderByEventTimeAsc(badSess))
                .thenThrow(new RuntimeException("DB error for bad session"));
        when(sessionRepository.findBySessionId(goodSess)).thenReturn(Optional.empty());

        int processed = service.aggregate();

        assertEquals(1, processed, "坏session不应影响好session的处理");
        verify(sessionRepository).save(any(Session.class));
    }

    private Map<String, Session> captureSavedSessions(int times) {
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository, times(times)).save(captor.capture());
        Map<String, Session> map = new HashMap<>();
        for (Session s : captor.getAllValues()) {
            map.put(s.getSessionId(), s);
        }
        return map;
    }

    private UserEvent createEvent(String eventName, LocalDateTime time) {
        return createEvent("user-default", eventName, "sess-default", time);
    }

    private UserEvent createEvent(String userId, String eventName, String sessionId, LocalDateTime eventTime) {
        UserEvent e = new UserEvent();
        e.setUserId(userId);
        e.setEventName(eventName);
        e.setSessionId(sessionId);
        e.setEventTime(eventTime);
        return e;
    }
}
