package com.example.Kadr.controller;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository users;
    private final ProfileService profileService;

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("hasOrganizer", profileService.hasOrganizer(user));
        return "profile";
    }

    @PostMapping("/profile/username")
    public String updateUsername(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam("username") String username,
                                 @RequestParam("currentPassword") String currentPassword,
                                 RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        try {
            profileService.updateUsername(user, username, currentPassword);
            ra.addFlashAttribute("ok", "Логин обновлён");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile#edit-username";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/email")
    public String updateEmail(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam("email") String email,
                              @RequestParam("currentPassword") String currentPassword,
                              RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        try {
            profileService.updateEmail(user, email, currentPassword);
            ra.addFlashAttribute("ok", "Email обновлён");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile#edit-email";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        try {
            profileService.changePassword(user, currentPassword, newPassword, confirmNewPassword);
            ra.addFlashAttribute("ok", "Пароль изменён");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile#edit-password";
        }
        return "redirect:/profile";
    }
    @PostMapping("/profile/organizer")
    public String createOrganizer(@AuthenticationPrincipal UserDetails principal,
                                  @RequestParam("organizationName") String organizationName,
                                  @RequestParam("contactEmail") String contactEmail,
                                  RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        try {
            profileService.createOrganizerForUser(user, organizationName, contactEmail);
            ra.addFlashAttribute("ok", "Организатор создан и закреплён за вашим профилем.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile#new-organizer";
        }
        return "redirect:/profile";
    }
}
