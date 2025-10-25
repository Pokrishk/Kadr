package com.example.Kadr.repository;

import com.example.Kadr.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findTop20ByEvent_IdOrderByCreatedAtDesc(Long eventId);
    @EntityGraph(attributePaths = {"event", "user"})
    Page<Review> findByRatingOrderByCreatedAtDesc(Integer rating, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByEvent_IdOrderByCreatedAtDesc(Long eventId, Pageable pageable);
    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);
}
