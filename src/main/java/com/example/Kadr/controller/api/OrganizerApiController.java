package com.example.Kadr.controller.api;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.repository.OrganizerRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizers")
@Tag(name = "Организаторы", description = "API для управления организаторами")
public class OrganizerApiController extends AbstractCrudApiController<Organizer> {

    private final OrganizerRepository organizerRepository;

    public OrganizerApiController(OrganizerRepository organizerRepository) {
        this.organizerRepository = organizerRepository;
    }

    @Override
    protected JpaRepository<Organizer, Long> getRepository() {
        return organizerRepository;
    }

    @Override
    protected String getResourceName() {
        return "Организатор";
    }
}