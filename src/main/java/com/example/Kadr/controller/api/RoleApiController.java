package com.example.Kadr.controller.api;

import com.example.Kadr.model.Role;
import com.example.Kadr.repository.RoleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Роли", description = "Управление ролями пользователей")
public class RoleApiController extends AbstractCrudApiController<Role> {

    private final RoleRepository roleRepository;

    public RoleApiController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    protected JpaRepository<Role, Long> getRepository() {
        return roleRepository;
    }

    @Override
    protected String getResourceName() {
        return "Роль";
    }
}