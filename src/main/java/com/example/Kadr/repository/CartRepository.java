package com.example.Kadr.repository;

import com.example.Kadr.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository <Cart, Long> {
}
