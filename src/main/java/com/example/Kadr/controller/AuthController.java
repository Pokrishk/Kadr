package com.example.Kadr.controller;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.User;
import com.example.Kadr.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;


@Controller
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder encoder;

    public AuthController(AuthService authService, PasswordEncoder encoder) {
        this.authService = authService;
        this.encoder = encoder;
    }

    @InitBinder("user")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("username","email","password","confirmPassword");
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("user") @Validated(ValidationGroups.OnRegister.class) User user,
                             BindingResult br, Model model) {

        if (!br.hasFieldErrors("username") && authService.usernameExists(user.getUsername())) {
            br.rejectValue("username", "username.exists", "Логин уже занят");
        }
        if (!br.hasFieldErrors("email") && authService.emailExists(user.getEmail())) {
            br.rejectValue("email", "email.exists", "Email уже зарегистрирован");
        }

        if (br.hasErrors()) return "registration";

        authService.registerEntity(user, encoder.encode(user.getPassword()));
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }
}
