package com.example.Kadr.controller;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final UserRepository users;
    private final CartService cartService;

    public CartController(UserRepository users, CartService cartService) {
        this.users = users;
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        model.addAttribute("cartItems", cartService.getUserCart(user));
        return "cart";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        cartService.deleteItem(user, id);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        var items = cartService.getUserCart(user);
        if (items.isEmpty()) return "redirect:/cart";
        cartService.createOrderFromCart(user, items);
        return "redirect:/orders?success";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("ticketId") Long ticketId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = users.findByUsername(principal.getUsername()).orElseThrow();

        try {
            cartService.addToCart(user, ticketId, quantity);
            ra.addFlashAttribute("notice", "Товар добавлен в корзину.");
            return "redirect:/cart";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("cartForm", Map.of("ticketId", ticketId, "quantity", quantity));
            return "redirect:/cart";
        }
    }
}
