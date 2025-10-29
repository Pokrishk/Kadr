package com.example.Kadr.controller.api;

import com.example.Kadr.model.Review;
import com.example.Kadr.repository.ReviewRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Отзывы", description = "API для отзывов пользователей")
public class ReviewApiController extends AbstractCrudApiController<Review> {

    private final ReviewRepository reviewRepository;

    public ReviewApiController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    protected JpaRepository<Review, Long> getRepository() {
        return reviewRepository;
    }

    @Override
    protected String getResourceName() {
        return "Отзыв";
    }
}