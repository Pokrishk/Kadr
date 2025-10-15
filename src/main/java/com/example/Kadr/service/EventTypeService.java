package com.example.Kadr.service;

import com.example.Kadr.model.EventType;
import com.example.Kadr.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;

    @Transactional(readOnly = true)
    public List<EventType> getRandomTypes(int poolSize, int limit) {
        var page = eventTypeRepository.findAll(
                PageRequest.of(0, Math.max(poolSize, limit), Sort.by("id").descending())
        );

        List<EventType> pool = new ArrayList<>(page.getContent());
        Collections.shuffle(pool);
        return pool.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventType> findAll() { return eventTypeRepository.findAll(); }
}
