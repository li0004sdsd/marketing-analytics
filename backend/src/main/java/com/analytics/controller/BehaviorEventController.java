package com.analytics.controller;

import com.analytics.dto.BehaviorEventRequest;
import com.analytics.entity.BehaviorEvent;
import com.analytics.service.BehaviorEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class BehaviorEventController {
    private final BehaviorEventService behaviorEventService;

    public BehaviorEventController(BehaviorEventService behaviorEventService) {
        this.behaviorEventService = behaviorEventService;
    }

    @GetMapping
    public ResponseEntity<List<BehaviorEvent>> getAll() {
        return ResponseEntity.ok(behaviorEventService.getAll());
    }

    @PostMapping("/track")
    public ResponseEntity<BehaviorEvent> track(@Valid @RequestBody BehaviorEventRequest request) {
        return ResponseEntity.ok(behaviorEventService.track(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BehaviorEvent>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(behaviorEventService.getByUser(userId));
    }

    @GetMapping("/type/{eventName}")
    public ResponseEntity<List<BehaviorEvent>> getByEventName(@PathVariable String eventName) {
        return ResponseEntity.ok(behaviorEventService.getByEventName(eventName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        behaviorEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
