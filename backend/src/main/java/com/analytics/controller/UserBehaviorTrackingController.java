package com.analytics.controller;

import com.analytics.dto.TrackEventRequest;
import com.analytics.service.UserBehaviorTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user-behavior")
public class UserBehaviorTrackingController {
    private final UserBehaviorTrackingService trackingService;

    public UserBehaviorTrackingController(UserBehaviorTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackEvent(@Valid @RequestBody TrackEventRequest request) {
        trackingService.recordEvent(
                request.getUserId(),
                request.getEventName(),
                request.getSessionId(),
                request.getProperties(),
                request.getEventTime()
        );
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    @GetMapping("/user/{userId}/event-count")
    public ResponseEntity<Map<String, Long>> getEventCountByUser(
            @PathVariable String userId,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        Long count;
        if (start != null && end != null) {
            count = trackingService.getEventCountByUser(userId, start, end);
        } else {
            count = trackingService.getEventCountByUser(userId);
        }
        return ResponseEntity.ok(Map.of("count", count));
    }
}
