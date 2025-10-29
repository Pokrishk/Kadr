package com.example.Kadr.controller.api;

import com.example.Kadr.model.UserLog;
import com.example.Kadr.repository.UserLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-logs")
@Tag(name = "Связи пользователей и журналов", description = "Работа с таблицей user_logs")
public class UserLogApiController extends AbstractCrudApiController<UserLog> {

    private final UserLogRepository userLogRepository;

    public UserLogApiController(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    @Override
    protected JpaRepository<UserLog, Long> getRepository() {
        return userLogRepository;
    }

    @Override
    protected String getResourceName() {
        return "Связь пользователя и записи";
    }
}