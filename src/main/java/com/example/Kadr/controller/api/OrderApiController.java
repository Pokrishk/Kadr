package com.example.Kadr.controller.api;

import com.example.Kadr.model.OrderEntity;
import com.example.Kadr.model.OrderItem;
import com.example.Kadr.repository.OrderEntityRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Заказы", description = "Управление заказами и их позициями")
public class OrderApiController extends AbstractCrudApiController<OrderEntity> {

    private final OrderEntityRepository orderEntityRepository;

    public OrderApiController(OrderEntityRepository orderEntityRepository) {
        this.orderEntityRepository = orderEntityRepository;
    }

    @Override
    protected JpaRepository<OrderEntity, Long> getRepository() {
        return orderEntityRepository;
    }

    @Override
    protected String getResourceName() {
        return "Заказ";
    }

    @Override
    protected void prepareForCreate(OrderEntity entity) {
        if (entity.getItems() != null) {
            for (OrderItem item : entity.getItems()) {
                if (item != null) {
                    item.setOrder(entity);
                }
            }
        }
    }
}