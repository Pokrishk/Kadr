package com.example.Kadr.controller;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.service.ReviewService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository users;

    @PostMapping("/{eventId}/reviews")
    public String addReview(@PathVariable Long eventId,
                            @RequestParam("rating") Integer rating,
                            @RequestParam("body") String body,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();

        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            ra.addFlashAttribute("error", "Текст отзыва не должен быть пустым");
            ra.addFlashAttribute("reviewForm", java.util.Map.of("rating", rating, "body", body));
            return "redirect:/events/" + eventId;
        }
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);
        int safeRating = Math.max(1, Math.min(5, rating == null ? 0 : rating));

        try {
            reviewService.addReview(eventId, user, safeRating, trimmed);
            ra.addFlashAttribute("notice", "Спасибо за отзыв!");
            return "redirect:/events/" + eventId + "?reviewed";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("reviewForm", java.util.Map.of("rating", safeRating, "body", trimmed));
            return "redirect:/events/" + eventId;
        }
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteOwn(@PathVariable @Positive Long id,
                            @RequestParam("eventId") @Positive Long eventId,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        try {
            reviewService.deleteOwnReview(id, user);
            ra.addFlashAttribute("notice", "Отзыв удалён.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }
}
