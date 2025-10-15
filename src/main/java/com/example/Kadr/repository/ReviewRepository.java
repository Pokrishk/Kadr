package com.example.Kadr.repository;

import com.example.Kadr.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = {"event", "user"})
    Page<Review> findByRatingOrderByCreatedAtDesc(Integer rating, Pageable pageable);
    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByEvent_IdOrderByCreatedAtDesc(Long eventId, Pageable pageable);
}
