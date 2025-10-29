package com.example.Kadr.controller.api;

import com.example.Kadr.model.Cart;
import com.example.Kadr.repository.CartRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Корзины", description = "Работа с содержимым корзин пользователей")
public class CartApiController extends AbstractCrudApiController<Cart> {

    private final CartRepository cartRepository;

    public CartApiController(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    protected JpaRepository<Cart, Long> getRepository() {
        return cartRepository;
    }

    @Override
    protected String getResourceName() {
        return "Корзина";
    }
}