package com.analytics.service;

import com.analytics.entity.BudgetAlertConfig;
import com.analytics.entity.BudgetAlertRecord;
import com.analytics.repository.BudgetAlertRecordRepository;
import com.analytics.repository.BudgetAlertRepository;
import com.analytics.repository.TouchPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetMonitorService {

    private static final Logger log = LoggerFactory.getLogger(BudgetMonitorService.class);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BudgetAlertRepository budgetAlertRepository;
    private final BudgetAlertRecordRepository recordRepository;
    private final TouchPointRepository touchPointRepository;
    private final AlertDispatcher alertDispatcher;

    public BudgetMonitorService(BudgetAlertRepository budgetAlertRepository,
                                BudgetAlertRecordRepository recordRepository,
                                TouchPointRepository touchPointRepository,
                                AlertDispatcher alertDispatcher) {
        this.budgetAlertRepository = budgetAlertRepository;
        this.recordRepository = recordRepository;
        this.touchPointRepository = touchPointRepository;
        this.alertDispatcher = alertDispatcher;
    }

    @Transactional
    public int checkAndAlert() {
        List<BudgetAlertConfig> configs = budgetAlertRepository.findByEnabledTrue();
        if (configs.isEmpty()) {
            log.info("No enabled budget alert configs, skipping check");
            return 0;
        }

        List<Long> campaignIds = configs.stream()
                .map(BudgetAlertConfig::getCampaignId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, BigDecimal> spendMap = loadActualSpends(campaignIds);

        int alertedCount = 0;
        for (BudgetAlertConfig config : configs) {
            try {
                if (processSingleConfig(config, spendMap)) {
                    alertedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process budget alert for campaign {}: {}",
                        config.getCampaignId(), e.getMessage(), e);
            }
        }

        log.info("Budget monitor completed. {} alert(s) triggered for {} config(s).",
                alertedCount, configs.size());
        return alertedCount;
    }

    @Transactional
    public boolean checkAndAlert(Long campaignId) {
        if (campaignId == null) {
            return false;
        }
        Optional<BudgetAlertConfig> opt = budgetAlertRepository.findByCampaignId(campaignId);
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getEnabled())) {
            return false;
        }
        BudgetAlertConfig config = opt.get();
        Map<Long, BigDecimal> spendMap = loadActualSpends(Collections.singletonList(campaignId));
        return processSingleConfig(config, spendMap);
    }

    private Map<Long, BigDecimal> loadActualSpends(List<Long> campaignIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Long id : campaignIds) {
            map.put(id, BigDecimal.ZERO);
        }
        List<Object[]> rows = touchPointRepository.sumRevenueByCampaignIdIn(campaignIds);
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            BigDecimal value = (BigDecimal) row[1];
            map.put(id, value == null ? BigDecimal.ZERO : value);
        }
        return map;
    }

    boolean processSingleConfig(BudgetAlertConfig config, Map<Long, BigDecimal> spendMap) {
        Long campaignId = config.getCampaignId();
        BigDecimal actualSpend = spendMap.getOrDefault(campaignId, BigDecimal.ZERO);
        BigDecimal budgetLimit = nullSafe(config.getBudgetLimit());
        BigDecimal thresholdPercent = nullSafe(config.getAlertThresholdPercent());

        if (budgetLimit.signum() <= 0) {
            log.warn("Campaign {} has non-positive budget limit ({}), skipping", campaignId, budgetLimit);
            return false;
        }

        BigDecimal thresholdAmount = budgetLimit
                .multiply(thresholdPercent)
                .divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);

        if (actualSpend.compareTo(thresholdAmount) < 0) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (isAlertRecorded(campaignId, today, thresholdPercent)) {
            log.debug("Alert already sent today for campaign {} at threshold {}%, skipping.",
                    campaignId, thresholdPercent);
            return false;
        }

        String message = buildAlertMessage(campaignId, budgetLimit, thresholdPercent, actualSpend);

        BudgetAlertRecord record = buildRecord(config, actualSpend, budgetLimit, thresholdPercent,
                today, message);

        boolean persisted = persistAlertRecord(record);
        if (!persisted) {
            log.info("Alert record for campaign {} already exists (concurrent trigger), " +
                    "dispatching suppressed to avoid duplicates.", campaignId);
            return false;
        }

        try {
            alertDispatcher.dispatch(record);
        } catch (Exception dispatchEx) {
            log.error("Failed to dispatch alert for campaign {} after record persisted: {}",
                    campaignId, dispatchEx.getMessage(), dispatchEx);
        }

        return true;
    }

    private boolean isAlertRecorded(Long campaignId, LocalDate date, BigDecimal threshold) {
        return recordRepository.existsByCampaignIdAndAlertDateAndAlertThresholdPercent(
                campaignId, date, threshold
        );
    }

    private boolean persistAlertRecord(BudgetAlertRecord record) {
        try {
            recordRepository.save(record);
            return true;
        } catch (DataIntegrityViolationException dive) {
            return false;
        }
    }

    private BudgetAlertRecord buildRecord(BudgetAlertConfig config,
                                          BigDecimal actualSpend,
                                          BigDecimal budgetLimit,
                                          BigDecimal thresholdPercent,
                                          LocalDate alertDate,
                                          String message) {
        BudgetAlertRecord r = new BudgetAlertRecord();
        r.setCampaignId(config.getCampaignId());
        r.setAlertDate(alertDate);
        r.setAlertThresholdPercent(thresholdPercent);
        r.setActualSpend(actualSpend);
        r.setBudgetLimit(budgetLimit);
        r.setAlertChannels(config.getAlertChannels());
        r.setAlertMessage(message);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    String buildAlertMessage(Long campaignId, BigDecimal budgetLimit,
                             BigDecimal thresholdPercent, BigDecimal actualSpend) {
        BigDecimal actualPercent = budgetLimit.signum() > 0
                ? actualSpend.multiply(ONE_HUNDRED).divide(budgetLimit, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal overBy = actualSpend.subtract(budgetLimit);
        StringBuilder sb = new StringBuilder();
        sb.append("[预算超支告警] 活动ID=").append(campaignId);
        sb.append(" | 预算=").append(budgetLimit);
        sb.append(" | 实际花费=").append(actualSpend).append(" (已消耗 ").append(actualPercent).append("%)");
        sb.append(" | 告警阈值=").append(thresholdPercent).append("%");
        if (overBy.signum() > 0) {
            sb.append(" | 超支=").append(overBy);
        }
        return sb.toString();
    }

    BigDecimal calculateSpendPercent(BigDecimal actualSpend, BigDecimal budgetLimit) {
        if (budgetLimit == null || budgetLimit.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal spend = actualSpend == null ? BigDecimal.ZERO : actualSpend;
        return spend.multiply(ONE_HUNDRED).divide(budgetLimit, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
