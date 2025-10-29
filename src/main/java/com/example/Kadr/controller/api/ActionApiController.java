package com.example.Kadr.controller.api;

import com.example.Kadr.model.Action;
import com.example.Kadr.repository.ActionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
@Tag(name = "Акции", description = "CRUD-операции с действиями аудита")
public class ActionApiController extends AbstractCrudApiController<Action> {

    private final ActionRepository actionRepository;

    public ActionApiController(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    protected JpaRepository<Action, Long> getRepository() {
        return actionRepository;
    }

    @Override
    protected String getResourceName() {
        return "Действие";
    }
}