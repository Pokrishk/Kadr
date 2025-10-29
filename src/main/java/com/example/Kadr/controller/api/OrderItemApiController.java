package com.example.Kadr.controller.api;

import com.example.Kadr.model.OrderItem;
import com.example.Kadr.repository.OrderItemRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-items")
@Tag(name = "Позиции заказа", description = "CRUD для элементов заказа")
public class OrderItemApiController extends AbstractCrudApiController<OrderItem> {

    private final OrderItemRepository orderItemRepository;

    public OrderItemApiController(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    protected JpaRepository<OrderItem, Long> getRepository() {
        return orderItemRepository;
    }

    @Override
    protected String getResourceName() {
        return "Позиция заказа";
    }
}