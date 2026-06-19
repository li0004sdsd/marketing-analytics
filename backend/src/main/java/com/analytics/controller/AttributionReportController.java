package com.analytics.controller;

import com.analytics.dto.AttributionReportResponse;
import com.analytics.service.AttributionReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/attribution")
public class AttributionReportController {

    private final AttributionReportService attributionReportService;

    public AttributionReportController(AttributionReportService attributionReportService) {
        this.attributionReportService = attributionReportService;
    }

    @GetMapping("/report")
    public ResponseEntity<AttributionReportResponse> getReport(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime end) {

        AttributionReportResponse report;
        if (start != null && end != null) {
            report = attributionReportService.buildReport(teamId, start, end);
        } else {
            report = attributionReportService.buildReport(teamId);
        }
        return ResponseEntity.ok(report);
    }
}
