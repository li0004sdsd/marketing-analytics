package com.analytics.service;

import com.analytics.dto.AttributionReportEntry;
import com.analytics.dto.AttributionReportResponse;
import com.analytics.entity.Campaign;
import com.analytics.entity.CampaignTouchPoint;
import com.analytics.repository.CampaignRepository;
import com.analytics.repository.TouchPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttributionReportService {

    private static final Logger log = LoggerFactory.getLogger(AttributionReportService.class);

    private final CampaignRepository campaignRepository;
    private final TouchPointRepository touchPointRepository;

    public AttributionReportService(CampaignRepository campaignRepository,
                                    TouchPointRepository touchPointRepository) {
        this.campaignRepository = campaignRepository;
        this.touchPointRepository = touchPointRepository;
    }

    @Transactional(readOnly = true)
    public AttributionReportResponse buildReport(Long teamId) {
        return buildReport(teamId, null, null);
    }

    @Transactional(readOnly = true)
    public AttributionReportResponse buildReport(Long teamId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        long startNs = System.nanoTime();

        List<Campaign> campaigns = resolveCampaigns(teamId);

        if (campaigns.isEmpty()) {
            return emptyReport(teamId, rangeStart, rangeEnd);
        }

        List<Long> campaignIds = campaigns.stream()
                .map(Campaign::getId)
                .collect(Collectors.toList());

        Map<Long, Campaign> campaignIndex = campaigns.stream()
                .collect(Collectors.toMap(Campaign::getId, c -> c));

        List<CampaignTouchPoint> allTouchPoints = batchLoadTouchPoints(campaignIds, rangeStart, rangeEnd);

        Map<Long, List<CampaignTouchPoint>> touchPointsByCampaign = groupByCampaignId(allTouchPoints);

        List<AttributionReportEntry> entries = new ArrayList<>();
        long sumTouches = 0L;
        long sumConversions = 0L;
        BigDecimal sumWeight = BigDecimal.ZERO;
        BigDecimal sumRevenue = BigDecimal.ZERO;

        for (Campaign campaign : campaigns) {
            List<CampaignTouchPoint> touches = touchPointsByCampaign.getOrDefault(
                    campaign.getId(), Collections.emptyList()
            );

            AttributionReportEntry entry = buildEntry(campaign, touches);
            entries.add(entry);

            sumTouches += entry.getTotalTouchPoints();
            sumConversions += entry.getTotalConversions();
            sumWeight = sumWeight.add(nullSafe(entry.getTotalWeight()));
            sumRevenue = sumRevenue.add(nullSafe(entry.getTotalRevenue()));
        }

        AttributionReportResponse response = new AttributionReportResponse();
        response.setReportId(UUID.randomUUID().toString());
        response.setGeneratedAt(LocalDateTime.now());
        response.setRangeStart(rangeStart);
        response.setRangeEnd(rangeEnd);
        response.setTeamId(teamId);
        response.setCampaignCount(campaigns.size());
        response.setTotalTouchPoints(sumTouches);
        response.setTotalConversions(sumConversions);
        response.setGrandTotalWeight(sumWeight);
        response.setGrandTotalRevenue(sumRevenue);
        response.setCampaigns(entries);

        long ms = (System.nanoTime() - startNs) / 1_000_000;
        log.info("Attribution report built: {} campaigns, {} touchpoints, {}ms",
                campaigns.size(), allTouchPoints.size(), ms);
        return response;
    }

    private List<CampaignTouchPoint> batchLoadTouchPoints(List<Long> campaignIds,
                                                          LocalDateTime rangeStart,
                                                          LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null) {
            return touchPointRepository.findByCampaignIdInAndTouchTimeBetween(
                    campaignIds, rangeStart, rangeEnd
            );
        }
        return touchPointRepository.findByCampaignIdIn(campaignIds);
    }

    private Map<Long, List<CampaignTouchPoint>> groupByCampaignId(List<CampaignTouchPoint> all) {
        Map<Long, List<CampaignTouchPoint>> map = new HashMap<>();
        for (CampaignTouchPoint tp : all) {
            map.computeIfAbsent(tp.getCampaignId(), k -> new ArrayList<>()).add(tp);
        }
        for (List<CampaignTouchPoint> list : map.values()) {
            list.sort(Comparator.comparing(CampaignTouchPoint::getTouchTime));
        }
        return map;
    }

    AttributionReportEntry buildEntry(Campaign campaign, List<CampaignTouchPoint> touches) {
        AttributionReportEntry entry = new AttributionReportEntry();
        entry.setCampaignId(campaign.getId());
        entry.setCampaignName(campaign.getName());

        long count = touches.size();
        entry.setTotalTouchPoints(count);

        if (count == 0) {
            entry.setTotalConversions(0L);
            entry.setTotalWeight(BigDecimal.ZERO);
            entry.setTotalRevenue(BigDecimal.ZERO);
            entry.setAvgWeightPerTouch(BigDecimal.ZERO);
            entry.setFirstTouchAt(null);
            entry.setLastTouchAt(null);
            entry.setChannelWeights(new LinkedHashMap<>());
            entry.setTouchPoints(new ArrayList<>());
            return entry;
        }

        LocalDateTime first = touches.get(0).getTouchTime();
        LocalDateTime last = touches.get(touches.size() - 1).getTouchTime();
        entry.setFirstTouchAt(first);
        entry.setLastTouchAt(last);

        long conversions = 0L;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, BigDecimal> channelWeights = new LinkedHashMap<>();
        List<AttributionReportEntry.TouchPointBreakdown> breakdowns = new ArrayList<>(touches.size());

        for (CampaignTouchPoint tp : touches) {
            BigDecimal w = nullSafe(tp.getAttributionWeight());
            BigDecimal r = nullSafe(tp.getRevenue());

            totalWeight = totalWeight.add(w);
            totalRevenue = totalRevenue.add(r);
            if (tp.getConversionId() != null && !tp.getConversionId().isBlank()) {
                conversions++;
            }
            channelWeights.merge(tp.getChannel(), w, BigDecimal::add);

            AttributionReportEntry.TouchPointBreakdown b = new AttributionReportEntry.TouchPointBreakdown();
            b.setUserId(tp.getUserId());
            b.setChannel(tp.getChannel());
            b.setWeight(w);
            b.setOrder(tp.getTouchOrder());
            b.setTouchTime(tp.getTouchTime());
            b.setConversionId(tp.getConversionId());
            b.setRevenue(r);
            breakdowns.add(b);
        }

        entry.setTotalConversions(conversions);
        entry.setTotalWeight(totalWeight);
        entry.setTotalRevenue(totalRevenue);
        entry.setAvgWeightPerTouch(count > 0
                ? totalWeight.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        entry.setChannelWeights(channelWeights);
        entry.setTouchPoints(breakdowns);

        return entry;
    }

    private List<Campaign> resolveCampaigns(Long teamId) {
        if (teamId == null) {
            return campaignRepository.findAll();
        }
        return campaignRepository.findByTeamId(teamId);
    }

    private AttributionReportResponse emptyReport(Long teamId, LocalDateTime start, LocalDateTime end) {
        AttributionReportResponse r = new AttributionReportResponse();
        r.setReportId(UUID.randomUUID().toString());
        r.setGeneratedAt(LocalDateTime.now());
        r.setRangeStart(start);
        r.setRangeEnd(end);
        r.setTeamId(teamId);
        r.setCampaignCount(0);
        r.setTotalTouchPoints(0L);
        r.setTotalConversions(0L);
        r.setGrandTotalWeight(BigDecimal.ZERO);
        r.setGrandTotalRevenue(BigDecimal.ZERO);
        r.setCampaigns(new ArrayList<>());
        return r;
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
