package com.analytics.service;

import com.analytics.dto.EventTypeRequest;
import com.analytics.entity.EventType;
import com.analytics.repository.EventTypeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventTypeService {
    private final EventTypeRepository eventTypeRepository;

    public EventTypeService(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    public List<EventType> getAll() {
        return eventTypeRepository.findAll();
    }

    public List<EventType> getActive() {
        return eventTypeRepository.findByActiveTrue();
    }

    public EventType getById(Long id) {
        return eventTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("EventType not found: " + id));
    }

    public EventType create(EventTypeRequest request) {
        if (eventTypeRepository.existsByName(request.getName())) {
            throw new RuntimeException("Event type name already exists");
        }
        EventType et = new EventType();
        et.setName(request.getName());
        et.setDescription(request.getDescription());
        et.setCategory(request.getCategory());
        et.setActive(request.getActive() != null ? request.getActive() : true);
        return eventTypeRepository.save(et);
    }

    public EventType update(Long id, EventTypeRequest request) {
        EventType et = getById(id);
        if (!et.getName().equals(request.getName()) && eventTypeRepository.existsByName(request.getName())) {
            throw new RuntimeException("Event type name already exists");
        }
        et.setName(request.getName());
        et.setDescription(request.getDescription());
        et.setCategory(request.getCategory());
        if (request.getActive() != null) et.setActive(request.getActive());
        return eventTypeRepository.save(et);
    }

    public void delete(Long id) {
        EventType et = getById(id);
        eventTypeRepository.delete(et);
    }
}
