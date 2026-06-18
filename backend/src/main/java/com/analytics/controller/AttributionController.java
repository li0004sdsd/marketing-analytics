package com.analytics.controller;

import com.analytics.dto.AttributionConfigRequest;
import com.analytics.dto.AttributionResult;
import com.analytics.entity.AttributionConfig;
import com.analytics.service.AttributionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/attribution")
public class AttributionController {
    private final AttributionService attributionService;

    public AttributionController(AttributionService attributionService) {
        this.attributionService = attributionService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<AttributionResult> calculate(
            @RequestBody List<String> channels,
            @RequestParam(required = false) Long configId) {
        return ResponseEntity.ok(attributionService.calculate(channels, configId));
    }

    @GetMapping("/configs")
    public ResponseEntity<List<AttributionConfig>> getAllConfigs() {
        return ResponseEntity.ok(attributionService.getAllConfigs());
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<AttributionConfig> getConfig(@PathVariable Long id) {
        return attributionService.getConfig(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/configs")
    public ResponseEntity<AttributionConfig> createConfig(@Valid @RequestBody AttributionConfigRequest request) {
        return ResponseEntity.ok(attributionService.createConfig(request));
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<AttributionConfig> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AttributionConfigRequest request) {
        return ResponseEntity.ok(attributionService.updateConfig(id, request));
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        attributionService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
}
