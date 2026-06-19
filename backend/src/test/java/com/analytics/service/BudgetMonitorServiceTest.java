package com.analytics.service;

import com.analytics.entity.BudgetAlertConfig;
import com.analytics.entity.BudgetAlertRecord;
import com.analytics.repository.BudgetAlertRecordRepository;
import com.analytics.repository.BudgetAlertRepository;
import com.analytics.repository.TouchPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetMonitorServiceTest {

    @Mock
    private BudgetAlertRepository budgetAlertRepository;
    @Mock
    private BudgetAlertRecordRepository recordRepository;
    @Mock
    private TouchPointRepository touchPointRepository;
    @Mock
    private AlertDispatcher alertDispatcher;

    private BudgetMonitorService service;

    @BeforeEach
    void setUp() {
        service = new BudgetMonitorService(
                budgetAlertRepository, recordRepository, touchPointRepository, alertDispatcher
        );
    }

    @Test
    @DisplayName("花费达阈值时触发告警：调用dispatch并保存告警记录")
    void testCheckAndAlert_SpendOverThreshold_TriggersAlert() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("850"));
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
                eq(100L), any(LocalDate.class), eq(new BigDecimal("80"))))
                .thenReturn(false);
        when(recordRepository.save(any(BudgetAlertRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int count = service.checkAndAlert();

        assertEquals(1, count);

        ArgumentCaptor<BudgetAlertRecord> recCaptor = ArgumentCaptor.forClass(BudgetAlertRecord.class);
        verify(recordRepository).save(recCaptor.capture());
        BudgetAlertRecord saved = recCaptor.getValue();
        assertEquals(Long.valueOf(100L), saved.getCampaignId());
        assertEquals(0, new BigDecimal("850").compareTo(saved.getActualSpend()));
        assertEquals(0, new BigDecimal("1000").compareTo(saved.getBudgetLimit()));
        assertEquals(0, new BigDecimal("80").compareTo(saved.getAlertThresholdPercent()));
        assertEquals(LocalDate.now(), saved.getAlertDate());
        assertTrue(saved.getAlertMessage().contains("100"));
        assertTrue(saved.getAlertMessage().contains("850"));

        verify(alertDispatcher).dispatch(saved);
    }

    @Test
    @DisplayName("花费低于阈值时不触发：无dispatch、无record保存")
    void testCheckAndAlert_SpendBelowThreshold_NoAlert() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("500"));

        int count = service.checkAndAlert();

        assertEquals(0, count);
        verify(recordRepository, never()).existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
                any(), any(), any());
        verify(recordRepository, never()).save(any());
        verify(alertDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("幂等控制：同一天同阈值只告警一次，第二次不重复")
    void testCheckAndAlert_SameDaySameThreshold_AlertOnce() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("900"));
        when(recordRepository.save(any(BudgetAlertRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
                eq(100L), any(LocalDate.class), eq(new BigDecimal("80"))))
                .thenReturn(false)
                .thenReturn(true);

        int first = service.checkAndAlert();
        assertEquals(1, first);

        int second = service.checkAndAlert();
        assertEquals(0, second);

        verify(recordRepository, times(2))
                .existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
                        eq(100L), any(LocalDate.class), eq(new BigDecimal("80")));
        verify(recordRepository, times(1)).save(any());
        verify(alertDispatcher, times(1)).dispatch(any());
    }

    @Test
    @DisplayName("同一天不同阈值可分别触发：80%和100%独立告警")
    void testCheckAndAlert_SameDayDifferentThresholds_BothAlert() {
        BudgetAlertConfig config80 = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        BudgetAlertConfig config100 = makeConfig(200L, new BigDecimal("1000"),
                new BigDecimal("100"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Arrays.asList(config80, config100));

        mockMultipleSpends(
                new Object[]{100L, new BigDecimal("850")},
                new Object[]{200L, new BigDecimal("1050")}
        );
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);
        when(recordRepository.save(any(BudgetAlertRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int count = service.checkAndAlert();
        assertEquals(2, count);

        ArgumentCaptor<BudgetAlertRecord> captor = ArgumentCaptor.forClass(BudgetAlertRecord.class);
        verify(recordRepository, times(2)).save(captor.capture());
        List<BudgetAlertRecord> all = captor.getAllValues();

        BudgetAlertRecord r80 = findByCampaign(all, 100L);
        assertEquals(0, new BigDecimal("80").compareTo(r80.getAlertThresholdPercent()));
        assertEquals(0, new BigDecimal("850").compareTo(r80.getActualSpend()));

        BudgetAlertRecord r100 = findByCampaign(all, 200L);
        assertEquals(0, new BigDecimal("100").compareTo(r100.getAlertThresholdPercent()));
        assertEquals(0, new BigDecimal("1050").compareTo(r100.getActualSpend()));
    }

    @Test
    @DisplayName("联合唯一约束冲突：save抛DataIntegrityViolationException时返回false，不再dispatch")
    void testCheckAndAlert_UniqueConstraintViolation_ReturnsFalse_NoDispatch() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("900"));
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);
        when(recordRepository.save(any(BudgetAlertRecord.class)))
                .thenThrow(new DataIntegrityViolationException("UK constraint violated"));

        int count = service.checkAndAlert();

        assertEquals(0, count);
        verify(alertDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("花费恰好等于阈值：80% = 800 应触发告警")
    void testCheckAndAlert_SpendEqualsThreshold_TriggersAlert() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("800"));
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.checkAndAlert();
        assertEquals(1, count);
        verify(alertDispatcher).dispatch(any());
    }

    @Test
    @DisplayName("预算为0或负数：跳过不触发")
    void testCheckAndAlert_NonPositiveBudget_Skipped() {
        BudgetAlertConfig zeroBudget = makeConfig(100L, BigDecimal.ZERO,
                new BigDecimal("80"), "EMAIL");
        BudgetAlertConfig negativeBudget = makeConfig(200L, new BigDecimal("-100"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Arrays.asList(zeroBudget, negativeBudget));

        mockMultipleSpends(
                new Object[]{100L, new BigDecimal("1000")},
                new Object[]{200L, new BigDecimal("1000")}
        );

        int count = service.checkAndAlert();

        assertEquals(0, count);
        verify(recordRepository, never()).existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any());
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("未配置活动：返回0不报错")
    void testCheckAndAlert_NoConfigs_ReturnsZero() {
        when(budgetAlertRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        int count = service.checkAndAlert();

        assertEquals(0, count);
        verify(touchPointRepository, never()).sumRevenueByCampaignIdIn(any());
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("单campaign检查：checkAndAlert(campaignId)路径")
    void testCheckAndAlert_SingleCampaign_TriggersAlert() {
        BudgetAlertConfig config = makeConfig(55L, new BigDecimal("5000"),
                new BigDecimal("90"), "SMS,EMAIL");
        when(budgetAlertRepository.findByCampaignId(55L)).thenReturn(Optional.of(config));

        mockSpend(55L, new BigDecimal("4800"));
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.checkAndAlert(55L);

        assertTrue(result);
        verify(budgetAlertRepository).findByCampaignId(55L);
        verify(alertDispatcher).dispatch(any());
    }

    @Test
    @DisplayName("单campaign检查：未找到配置或禁用时返回false")
    void testCheckAndAlert_SingleCampaign_NoConfig_ReturnsFalse() {
        when(budgetAlertRepository.findByCampaignId(999L)).thenReturn(Optional.empty());
        assertFalse(service.checkAndAlert(999L));

        BudgetAlertConfig disabled = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        disabled.setEnabled(false);
        when(budgetAlertRepository.findByCampaignId(100L)).thenReturn(Optional.of(disabled));

        assertFalse(service.checkAndAlert(100L));
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("告警渠道解析：多渠道组合 SMS,EMAIL,WEBHOOK dispatch均执行")
    void testCheckAndAlert_MultiChannels_DispatchedAll() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL,  SMS , WEBHOOK, ");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        mockSpend(100L, new BigDecimal("900"));
        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(alertDispatcher).dispatch(any());

        service.checkAndAlert();

        ArgumentCaptor<BudgetAlertRecord> captor = ArgumentCaptor.forClass(BudgetAlertRecord.class);
        verify(alertDispatcher).dispatch(captor.capture());
        BudgetAlertRecord r = captor.getValue();
        assertEquals("EMAIL,  SMS , WEBHOOK, ", r.getAlertChannels());
    }

    @Test
    @DisplayName("calculateSpendPercent: 正确计算百分比")
    void testCalculateSpendPercent() {
        assertEquals(0, new BigDecimal("85.00").compareTo(
                service.calculateSpendPercent(new BigDecimal("850"), new BigDecimal("1000"))));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                service.calculateSpendPercent(new BigDecimal("1000"), new BigDecimal("1000"))));
        assertEquals(0, new BigDecimal("120.50").compareTo(
                service.calculateSpendPercent(new BigDecimal("241"), new BigDecimal("200"))));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculateSpendPercent(new BigDecimal("500"), BigDecimal.ZERO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculateSpendPercent(null, new BigDecimal("100"))));
    }

    @Test
    @DisplayName("buildAlertMessage: 包含campaignId、limit、spend、threshold、超支金额")
    void testBuildAlertMessage_Content() {
        String msg = service.buildAlertMessage(
                42L, new BigDecimal("2000"), new BigDecimal("90"), new BigDecimal("2100"));
        assertTrue(msg.contains("42"));
        assertTrue(msg.contains("2000"));
        assertTrue(msg.contains("2100"));
        assertTrue(msg.contains("90"));
        assertTrue(msg.contains("100") || msg.contains("105"));
        assertTrue(msg.contains("超支"));

        String noOver = service.buildAlertMessage(
                1L, new BigDecimal("1000"), new BigDecimal("80"), new BigDecimal("820"));
        assertTrue(noOver.contains("82%") || noOver.contains("82"),
                "应包含实际百分比，实际: " + noOver);
    }

    @Test
    @DisplayName("实际花费未入库（NULL）：按0处理不崩溃不告警")
    void testCheckAndAlert_NullSpend_TreatedAsZero() {
        BudgetAlertConfig config = makeConfig(100L, new BigDecimal("1000"),
                new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue())
                .thenReturn(Collections.singletonList(config));

        when(touchPointRepository.sumRevenueByCampaignIdIn(any()))
                .thenReturn(Collections.emptyList());

        assertEquals(0, service.checkAndAlert());
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("某配置异常不影响其他配置：3个中2个正常触发，中间1个抛异常")
    void testCheckAndAlert_OneConfigException_DoesNotBreakOthers() {
        BudgetAlertConfig c1 = makeConfig(1L, new BigDecimal("1000"), new BigDecimal("80"), "EMAIL");
        BudgetAlertConfig bad = makeConfig(2L, new BigDecimal("1000"), new BigDecimal("80"), "EMAIL");
        BudgetAlertConfig c3 = makeConfig(3L, new BigDecimal("1000"), new BigDecimal("80"), "EMAIL");
        when(budgetAlertRepository.findByEnabledTrue()).thenReturn(Arrays.asList(c1, bad, c3));

        when(touchPointRepository.sumRevenueByCampaignIdIn(any())).thenReturn(Arrays.asList(
                new Object[]{1L, new BigDecimal("900")},
                new Object[]{2L, new BigDecimal("900")},
                new Object[]{3L, new BigDecimal("900")}
        ));

        when(recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(any(), any(), any()))
                .thenReturn(false);

        when(recordRepository.save(any(BudgetAlertRecord.class)))
                .thenAnswer(inv -> {
                    BudgetAlertRecord r = inv.getArgument(0);
                    if (Long.valueOf(2L).equals(r.getCampaignId())) {
                        throw new RuntimeException("DB boom");
                    }
                    return r;
                });

        int count = service.checkAndAlert();
        assertEquals(2, count, "bad异常不应影响c1和c3的正常告警");
    }

    private BudgetAlertRecord findByCampaign(List<BudgetAlertRecord> list, Long campaignId) {
        return list.stream()
                .filter(r -> campaignId.equals(r.getCampaignId()))
                .findFirst()
                .orElseThrow();
    }

    private BudgetAlertConfig makeConfig(Long campaignId, BigDecimal limit,
                                         BigDecimal threshold, String channels) {
        BudgetAlertConfig c = new BudgetAlertConfig();
        c.setId(campaignId * 1000 + 1);
        c.setCampaignId(campaignId);
        c.setBudgetLimit(limit);
        c.setAlertThresholdPercent(threshold);
        c.setAlertChannels(channels);
        c.setEnabled(true);
        c.setCreatedAt(LocalDateTime.now().minusDays(1));
        c.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return c;
    }

    private void mockSpend(Long campaignId, BigDecimal spend) {
        when(touchPointRepository.sumRevenueByCampaignIdIn(any()))
                .thenReturn(Collections.singletonList(new Object[]{campaignId, spend}));
    }

    @SafeVarargs
    private final void mockMultipleSpends(Object[]... rows) {
        when(touchPointRepository.sumRevenueByCampaignIdIn(any()))
                .thenReturn(Arrays.asList(rows));
    }
}
