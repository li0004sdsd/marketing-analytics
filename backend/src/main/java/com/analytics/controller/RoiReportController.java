package com.analytics.controller;

import com.analytics.dto.ReportQuery;
import com.analytics.entity.RoiReport;
import com.analytics.service.ReportQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roi-reports")
public class RoiReportController {
    private final ReportQueryService reportQueryService;

    public RoiReportController(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @PostMapping("/query")
    public ResponseEntity<List<RoiReport>> queryReports(@RequestBody(required = false) ReportQuery query) {
        if (query == null) {
            query = new ReportQuery();
        }
        return ResponseEntity.ok(reportQueryService.buildQuery(query));
    }
}
