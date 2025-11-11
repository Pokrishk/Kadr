package com.example.Kadr.controller.api;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.service.UserSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
public class UserSettingsApiController {

    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;

    @PostMapping("/filters/{key}")
    public ResponseEntity<Void> saveFilters(@PathVariable String key,
                                            @AuthenticationPrincipal UserDetails principal,
                                            @RequestBody(required = false) JsonNode filters) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        userSettingsService.saveFilters(user, key, filters);
        return ResponseEntity.noContent().build();
    }
}