package com.example.Kadr.controller;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.OrderEntityRepository;
import com.example.Kadr.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {

    private final UserRepository users;
    private final OrderEntityRepository orders;

    public OrderController(UserRepository users, OrderEntityRepository orders) {
        this.users = users;
        this.orders = orders;
    }

    @GetMapping("/orders")
    public String viewOrders(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        model.addAttribute("orders", orders.findByUserOrderByCreatedAtDesc(user));
        return "orders";
    }
}
