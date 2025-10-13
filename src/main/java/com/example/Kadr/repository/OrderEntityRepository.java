package com.example.Kadr.repository;

import com.example.Kadr.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
}
