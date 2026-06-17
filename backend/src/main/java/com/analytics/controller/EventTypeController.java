package com.analytics.controller;

import com.analytics.dto.EventTypeRequest;
import com.analytics.entity.EventType;
import com.analytics.service.EventTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/event-types")
public class EventTypeController {
    private final EventTypeService eventTypeService;

    public EventTypeController(EventTypeService eventTypeService) {
        this.eventTypeService = eventTypeService;
    }

    @GetMapping
    public ResponseEntity<List<EventType>> getAll() {
        return ResponseEntity.ok(eventTypeService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<EventType>> getActive() {
        return ResponseEntity.ok(eventTypeService.getActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventType> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventTypeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EventType> create(@Valid @RequestBody EventTypeRequest request) {
        return ResponseEntity.ok(eventTypeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventType> update(@PathVariable Long id, @Valid @RequestBody EventTypeRequest request) {
        return ResponseEntity.ok(eventTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
