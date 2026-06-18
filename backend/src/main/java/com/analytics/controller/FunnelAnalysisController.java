package com.analytics.controller;

import com.analytics.dto.FunnelAnalysisResponse;
import com.analytics.service.FunnelAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funnel-analysis")
public class FunnelAnalysisController {
    private final FunnelAnalysisService funnelAnalysisService;

    public FunnelAnalysisController(FunnelAnalysisService funnelAnalysisService) {
        this.funnelAnalysisService = funnelAnalysisService;
    }

    @GetMapping("/{funnelId}/step-conversion")
    public ResponseEntity<FunnelAnalysisResponse> getStepConversion(@PathVariable Long funnelId) {
        return ResponseEntity.ok(funnelAnalysisService.getStepConversion(funnelId));
    }
}
