package com.analytics.controller;

import com.analytics.dto.AnalyticsResponse;
import com.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsResponse> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }
}
