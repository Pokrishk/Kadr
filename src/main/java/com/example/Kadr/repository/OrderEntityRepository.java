package com.example.Kadr.repository;

import com.example.Kadr.model.OrderEntity;
import com.example.Kadr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserOrderByCreatedAtDesc(User user);
}
