package com.example.Kadr.controller.api;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "CRUD-операции с пользователями системы")
public class UserApiController extends AbstractCrudApiController<User> {

    private final UserRepository userRepository;

    public UserApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    @Override
    protected String getResourceName() {
        return "Пользователь";
    }
}