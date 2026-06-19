package com.analytics.service;

import com.analytics.dto.AttributionReportEntry;
import com.analytics.dto.AttributionReportResponse;
import com.analytics.entity.Campaign;
import com.analytics.entity.CampaignTouchPoint;
import com.analytics.repository.CampaignRepository;
import com.analytics.repository.TouchPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributionReportServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private TouchPointRepository touchPointRepository;

    private AttributionReportService service;

    @BeforeEach
    void setUp() {
        service = new AttributionReportService(campaignRepository, touchPointRepository);
    }

    @Test
    @DisplayName("N+1消除：buildReport只调用findByCampaignIdIn一次，绝不调用findByCampaignId")
    void testBuildReport_NoNPlusOneQuery() {
        List<Campaign> campaigns = Arrays.asList(
                makeCampaign(1L, "Spring Sale", 10L),
                makeCampaign(2L, "Summer Launch", 10L),
                makeCampaign(3L, "Black Friday", 10L)
        );
        when(campaignRepository.findByTeamId(10L)).thenReturn(campaigns);

        LocalDateTime t = LocalDateTime.of(2025, 6, 1, 10, 0);
        List<CampaignTouchPoint> allTouches = Arrays.asList(
                makeTouch(1L, "u1", "SEM", t, 1, new BigDecimal("0.5"), null),
                makeTouch(2L, "u1", "社交", t, 1, new BigDecimal("0.3"), "conv-1"),
                makeTouch(1L, "u2", "SEO", t.plusHours(1), 1, new BigDecimal("0.2"), null),
                makeTouch(3L, "u3", "邮件", t.plusHours(2), 1, new BigDecimal("1.0"), "conv-2")
        );
        when(touchPointRepository.findByCampaignIdIn(anyCollection())).thenReturn(allTouches);

        AttributionReportResponse report = service.buildReport(10L);

        assertNotNull(report);
        assertEquals(3, report.getCampaignCount());

        verify(touchPointRepository, times(1)).findByCampaignIdIn(anyCollection());
        verify(touchPointRepository, never()).findByCampaignId(anyLong());

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(touchPointRepository).findByCampaignIdIn(captor.capture());
        assertEquals(3, captor.getValue().size());
        assertTrue(captor.getValue().containsAll(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("分组正确性：Map按campaign_id分组，触点归属正确，不跨campaign污染")
    void testBuildReport_TouchPointsGroupedCorrectly() {
        List<Campaign> campaigns = Arrays.asList(
                makeCampaign(1L, "Campaign-A", 5L),
                makeCampaign(2L, "Campaign-B", 5L)
        );
        when(campaignRepository.findByTeamId(5L)).thenReturn(campaigns);

        LocalDateTime t = LocalDateTime.of(2025, 6, 1, 10, 0);
        CampaignTouchPoint a1 = makeTouch(1L, "u1", "SEM", t, 1, new BigDecimal("0.4"), null);
        CampaignTouchPoint a2 = makeTouch(1L, "u2", "SEO", t, 1, new BigDecimal("0.6"), null);
        CampaignTouchPoint b1 = makeTouch(2L, "u1", "社交", t, 1, new BigDecimal("1.0"), "conv-X");
        when(touchPointRepository.findByCampaignIdIn(anyCollection())).thenReturn(Arrays.asList(a1, b1, a2));

        AttributionReportResponse report = service.buildReport(5L);

        Map<Long, AttributionReportEntry> entryMap = new HashMap<>();
        for (AttributionReportEntry e : report.getCampaigns()) {
            entryMap.put(e.getCampaignId(), e);
        }

        AttributionReportEntry entryA = entryMap.get(1L);
        assertNotNull(entryA);
        assertEquals(2L, entryA.getTotalTouchPoints());
        assertEquals(new BigDecimal("1.00"), entryA.getTotalWeight().stripTrailingZeros());
        assertEquals(0L, entryA.getTotalConversions());
        assertEquals(2, entryA.getTouchPoints().size());

        AttributionReportEntry entryB = entryMap.get(2L);
        assertNotNull(entryB);
        assertEquals(1L, entryB.getTotalTouchPoints());
        assertEquals(new BigDecimal("1.0"), entryB.getTotalWeight());
        assertEquals(1L, entryB.getTotalConversions());
        assertEquals(1, entryB.getTouchPoints().size());
        assertEquals("conv-X", entryB.getTouchPoints().get(0).getConversionId());
    }

    @Test
    @DisplayName("聚合指标：channelWeights按渠道合并，totalWeight/totalConversions/avgWeight正确")
    void testBuildReport_AggregationMetricsCorrect() {
        List<Campaign> campaigns = Collections.singletonList(makeCampaign(1L, "Mega Campaign", 1L));
        when(campaignRepository.findByTeamId(1L)).thenReturn(campaigns);

        LocalDateTime t = LocalDateTime.of(2025, 6, 1, 8, 0);
        List<CampaignTouchPoint> touches = Arrays.asList(
                makeTouch(1L, "u1", "SEM", t, 1, new BigDecimal("0.5"), null),
                makeTouch(1L, "u1", "社交", t.plusMinutes(30), 2, new BigDecimal("0.3"), "conv-a"),
                makeTouch(1L, "u1", "邮件", t.plusMinutes(60), 3, new BigDecimal("0.2"), null),
                makeTouch(1L, "u2", "SEM", t.plusHours(2), 1, new BigDecimal("0.5"), null),
                makeTouch(1L, "u2", "社交", t.plusHours(3), 2, new BigDecimal("0.5"), "conv-b")
        );
        when(touchPointRepository.findByCampaignIdIn(anyCollection())).thenReturn(touches);

        AttributionReportResponse report = service.buildReport(1L);
        assertEquals(1, report.getCampaignCount());
        assertEquals(5L, report.getTotalTouchPoints());
        assertEquals(2L, report.getTotalConversions());

        AttributionReportEntry entry = report.getCampaigns().get(0);
        assertEquals(5L, entry.getTotalTouchPoints());
        assertEquals(2L, entry.getTotalConversions());
        assertEquals(0, new BigDecimal("2.0").compareTo(entry.getTotalWeight()));

        Map<String, BigDecimal> cw = entry.getChannelWeights();
        assertEquals(0, new BigDecimal("1.0").compareTo(cw.get("SEM")));
        assertEquals(0, new BigDecimal("0.8").compareTo(cw.get("社交")));
        assertEquals(0, new BigDecimal("0.2").compareTo(cw.get("邮件")));

        assertEquals(0,
                new BigDecimal("0.400000").compareTo(entry.getAvgWeightPerTouch()),
                "avgWeightPerTouch = 2.0 / 5 = 0.4");
    }

    @Test
    @DisplayName("触点按touchTime排序：firstTouchAt/lastTouchAt取极值")
    void testBuildReport_FirstLastTouchTime() {
        List<Campaign> campaigns = Collections.singletonList(makeCampaign(1L, "X", 1L));
        when(campaignRepository.findByTeamId(1L)).thenReturn(campaigns);

        LocalDateTime t1 = LocalDateTime.of(2025, 6, 1, 8, 0);
        LocalDateTime t3 = LocalDateTime.of(2025, 6, 1, 18, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 6, 1, 12, 0);
        CampaignTouchPoint mid = makeTouch(1L, "u1", "A", t2, 1, BigDecimal.ONE, null);
        CampaignTouchPoint last = makeTouch(1L, "u1", "B", t3, 2, BigDecimal.ONE, null);
        CampaignTouchPoint first = makeTouch(1L, "u2", "C", t1, 1, BigDecimal.ONE, null);
        when(touchPointRepository.findByCampaignIdIn(anyCollection())).thenReturn(Arrays.asList(mid, last, first));

        AttributionReportEntry entry = service.buildReport(1L).getCampaigns().get(0);
        assertEquals(t1, entry.getFirstTouchAt(), "firstTouchAt应为最早的t1");
        assertEquals(t3, entry.getLastTouchAt(), "lastTouchAt应为最晚的t3");
        assertEquals(t1, entry.getTouchPoints().get(0).getTouchTime(),
                "touchPoints内部应按时间升序排列，第一个是t1");
        assertEquals(t3, entry.getTouchPoints().get(2).getTouchTime(),
                "touchPoints内部应按时间升序排列，最后一个是t3");
    }

    @Test
    @DisplayName("部分campaign无触点数据：零值填充，不崩溃")
    void testBuildReport_SomeCampaignsNoTouchPoints() {
        List<Campaign> campaigns = Arrays.asList(
                makeCampaign(1L, "Active", 7L),
                makeCampaign(2L, "Pending", 7L),
                makeCampaign(3L, "Draft", 7L)
        );
        when(campaignRepository.findByTeamId(7L)).thenReturn(campaigns);

        CampaignTouchPoint only = makeTouch(1L, "u1", "SEM",
                LocalDateTime.now(), 1, BigDecimal.ONE, "conv-1");
        when(touchPointRepository.findByCampaignIdIn(anyCollection()))
                .thenReturn(Collections.singletonList(only));

        AttributionReportResponse report = service.buildReport(7L);
        assertEquals(3, report.getCampaignCount());

        Map<String, AttributionReportEntry> byName = new HashMap<>();
        for (AttributionReportEntry e : report.getCampaigns()) {
            byName.put(e.getCampaignName(), e);
        }

        AttributionReportEntry active = byName.get("Active");
        assertEquals(1L, active.getTotalTouchPoints());
        assertEquals(1L, active.getTotalConversions());

        AttributionReportEntry pending = byName.get("Pending");
        assertEquals(0L, pending.getTotalTouchPoints());
        assertEquals(0L, pending.getTotalConversions());
        assertEquals(BigDecimal.ZERO, pending.getTotalWeight());
        assertEquals(BigDecimal.ZERO, pending.getAvgWeightPerTouch());
        assertNull(pending.getFirstTouchAt());
        assertNull(pending.getLastTouchAt());
        assertNotNull(pending.getChannelWeights());
        assertTrue(pending.getChannelWeights().isEmpty());
        assertNotNull(pending.getTouchPoints());
        assertTrue(pending.getTouchPoints().isEmpty());

        AttributionReportEntry draft = byName.get("Draft");
        assertEquals(0L, draft.getTotalTouchPoints());
    }

    @Test
    @DisplayName("空campaign列表：返回空报告，零值/空集合，不调用touchpoint查询")
    void testBuildReport_EmptyCampaigns_ReturnsEmptyReport() {
        when(campaignRepository.findByTeamId(999L)).thenReturn(Collections.emptyList());

        AttributionReportResponse report = service.buildReport(999L);

        assertNotNull(report);
        assertEquals(0, report.getCampaignCount());
        assertEquals(0L, report.getTotalTouchPoints());
        assertEquals(0L, report.getTotalConversions());
        assertEquals(BigDecimal.ZERO, report.getGrandTotalWeight());
        assertEquals(BigDecimal.ZERO, report.getGrandTotalRevenue());
        assertEquals(0, report.getCampaigns().size());
        assertNotNull(report.getReportId());
        assertNotNull(report.getGeneratedAt());

        verify(touchPointRepository, never()).findByCampaignIdIn(anyCollection());
        verify(touchPointRepository, never()).findByCampaignId(anyLong());
    }

    @Test
    @DisplayName("teamId=null时走findAll：等价于全量报告")
    void testBuildReport_NullTeamId_UsesFindAll() {
        List<Campaign> all = Arrays.asList(
                makeCampaign(1L, "C1", 10L),
                makeCampaign(2L, "C2", 20L)
        );
        when(campaignRepository.findAll()).thenReturn(all);
        when(touchPointRepository.findByCampaignIdIn(anyCollection()))
                .thenReturn(Collections.emptyList());

        AttributionReportResponse report = service.buildReport(null);

        verify(campaignRepository).findAll();
        verify(campaignRepository, never()).findByTeamId(any());
        assertEquals(2, report.getCampaignCount());
        assertNull(report.getTeamId());
    }

    @Test
    @DisplayName("带时间范围：走带BETWEEN的批量查询，不传范围走普通批量查询")
    void testBuildReport_WithTimeRange_UsesBetweenQuery() {
        List<Campaign> campaigns = Collections.singletonList(makeCampaign(1L, "T", 1L));
        when(campaignRepository.findByTeamId(1L)).thenReturn(campaigns);

        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 30, 23, 59, 59);
        when(touchPointRepository.findByCampaignIdInAndTouchTimeBetween(
                anyCollection(), any(), any())).thenReturn(Collections.emptyList());

        service.buildReport(1L, start, end);

        verify(touchPointRepository, times(1))
                .findByCampaignIdInAndTouchTimeBetween(anyCollection(), eq(start), eq(end));
        verify(touchPointRepository, never()).findByCampaignIdIn(anyCollection());

        service.buildReport(1L);

        verify(touchPointRepository, times(1)).findByCampaignIdIn(anyCollection());
    }

    @Test
    @DisplayName("revenue聚合：null安全，grandTotalRevenue/entry.totalRevenue正确求和")
    void testBuildReport_RevenueAggregation() {
        List<Campaign> campaigns = Arrays.asList(
                makeCampaign(1L, "C1", 1L),
                makeCampaign(2L, "C2", 1L)
        );
        when(campaignRepository.findByTeamId(1L)).thenReturn(campaigns);

        LocalDateTime t = LocalDateTime.now();
        when(touchPointRepository.findByCampaignIdIn(anyCollection())).thenReturn(Arrays.asList(
                makeTouchRevenue(1L, "u1", "A", t, 1, new BigDecimal("0.6"),
                        "c1", new BigDecimal("99.99")),
                makeTouchRevenue(1L, "u2", "B", t, 1, new BigDecimal("0.4"),
                        null, null),
                makeTouchRevenue(2L, "u3", "C", t, 1, new BigDecimal("1.0"),
                        "c2", new BigDecimal("200.00"))
        ));

        AttributionReportResponse report = service.buildReport(1L);
        assertEquals(0,
                new BigDecimal("299.99").compareTo(report.getGrandTotalRevenue()),
                "grandTotalRevenue = 99.99 + 0 + 200.00");

        Map<Long, AttributionReportEntry> byId = new HashMap<>();
        for (AttributionReportEntry e : report.getCampaigns()) {
            byId.put(e.getCampaignId(), e);
        }
        assertEquals(0,
                new BigDecimal("99.99").compareTo(byId.get(1L).getTotalRevenue()),
                "C1 revenue = 99.99");
        assertEquals(0,
                new BigDecimal("200.00").compareTo(byId.get(2L).getTotalRevenue()),
                "C2 revenue = 200.00");
    }

    private Campaign makeCampaign(Long id, String name, Long teamId) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setName(name);
        c.setTeamId(teamId);
        return c;
    }

    private CampaignTouchPoint makeTouch(Long campaignId, String userId, String channel,
                                         LocalDateTime touchTime, int order,
                                         BigDecimal weight, String conversionId) {
        return makeTouchRevenue(campaignId, userId, channel, touchTime, order,
                weight, conversionId, null);
    }

    private CampaignTouchPoint makeTouchRevenue(Long campaignId, String userId, String channel,
                                                LocalDateTime touchTime, int order,
                                                BigDecimal weight, String conversionId,
                                                BigDecimal revenue) {
        CampaignTouchPoint tp = new CampaignTouchPoint();
        tp.setCampaignId(campaignId);
        tp.setUserId(userId);
        tp.setChannel(channel);
        tp.setTouchTime(touchTime);
        tp.setTouchOrder(order);
        tp.setAttributionWeight(weight);
        tp.setConversionId(conversionId);
        tp.setRevenue(revenue);
        return tp;
    }
}
