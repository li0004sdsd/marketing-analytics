package com.analytics.service;

import com.analytics.entity.BudgetAlertRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);

    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_WEBHOOK = "WEBHOOK";
    public static final String CHANNEL_SLACK = "SLACK";
    public static final String CHANNEL_DELIMITER = ",";

    public void dispatch(BudgetAlertRecord record) {
        if (record == null || record.getAlertChannels() == null) {
            return;
        }
        List<String> channels = parseChannels(record.getAlertChannels());
        for (String channel : channels) {
            try {
                dispatchToChannel(channel.trim(), record);
            } catch (Exception e) {
                log.error("Failed to dispatch budget alert to channel {} for campaign {}: {}",
                        channel, record.getCampaignId(), e.getMessage(), e);
            }
        }
    }

    protected void dispatchToChannel(String channel, BudgetAlertRecord record) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        switch (channel.toUpperCase()) {
            case CHANNEL_EMAIL:
                sendEmail(record);
                break;
            case CHANNEL_SMS:
                sendSms(record);
                break;
            case CHANNEL_WEBHOOK:
                sendWebhook(record);
                break;
            case CHANNEL_SLACK:
                sendSlack(record);
                break;
            default:
                log.warn("Unknown alert channel: {}, skipping. Campaign={}", channel, record.getCampaignId());
        }
    }

    protected void sendEmail(BudgetAlertRecord record) {
        log.info("[EMAIL Alert] Campaign {} exceeds budget: limit={}, actual={}, threshold={}%",
                record.getCampaignId(), record.getBudgetLimit(),
                record.getActualSpend(), record.getAlertThresholdPercent());
    }

    protected void sendSms(BudgetAlertRecord record) {
        log.info("[SMS Alert] Campaign {} budget limit exceeded: {}% used. Spend={} / Limit={}",
                record.getCampaignId(), record.getAlertThresholdPercent(),
                record.getActualSpend(), record.getBudgetLimit());
    }

    protected void sendWebhook(BudgetAlertRecord record) {
        log.info("[WEBHOOK Alert] POST budget-alert event: campaignId={}, spend={}, limit={}, threshold={}",
                record.getCampaignId(), record.getActualSpend(),
                record.getBudgetLimit(), record.getAlertThresholdPercent());
    }

    protected void sendSlack(BudgetAlertRecord record) {
        log.info("[SLACK Alert] Campaign {} budget alert: {}% consumed ({} / {})",
                record.getCampaignId(), record.getAlertThresholdPercent(),
                record.getActualSpend(), record.getBudgetLimit());
    }

    public static List<String> parseChannels(String alertChannels) {
        if (alertChannels == null || alertChannels.isBlank()) {
            return List.of();
        }
        return Arrays.stream(alertChannels.split(CHANNEL_DELIMITER))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public static String formatChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return CHANNEL_EMAIL;
        }
        return String.join(CHANNEL_DELIMITER, channels);
    }
}
