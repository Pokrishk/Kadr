package com.example.Kadr.service;

import com.example.Kadr.model.Event;
import com.example.Kadr.model.Review;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.EventRepository;
import com.example.Kadr.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviews;
    private final EventRepository events;

    @Transactional(readOnly = true)
    public List<Review> getForEvent(Long eventId, int limit) {
        var list = reviews.findTop20ByEvent_IdOrderByCreatedAtDesc(eventId);
        return limit > 0 && list.size() > limit ? list.subList(0, limit) : list;
    }

    @Transactional
    public Review addReview(Long eventId, User user, int rating, String body) {
        Event event = events.findById(eventId).orElseThrow();

        if (reviews.existsByEvent_IdAndUser_Id(eventId, user.getId())) {
            throw new IllegalArgumentException("Вы уже оставляли отзыв к этому событию.");
        }
        var review = Review.builder()
                .event(event)
                .user(user)
                .rating(Math.max(1, Math.min(5, rating)))
                .body(body == null ? "" : body.trim())
                .build();
        if (review.getBody().length() > 1000) {
            review.setBody(review.getBody().substring(0, 1000));
        }
        try {
            var saved = reviews.save(review);
            recalcEventRating(event);
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Вы уже оставляли отзыв к этому событию.", e);
        }
    }

    @Transactional
    public void deleteOwnReview(Long reviewId, User user) {
        var r = reviews.findById(reviewId).orElseThrow();
        if (!r.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Нельзя удалять чужой отзыв.");
        }
        var event = r.getEvent();
        reviews.delete(r);
        recalcEventRating(event);
    }
    @Transactional(readOnly = true)
    public List<Review> getLatestFiveStar(int limit) {
        var page = reviews.findByRatingOrderByCreatedAtDesc(
                5, PageRequest.of(0, Math.max(1, limit))
        );
        return page.getContent();
    }

    private void recalcEventRating(Event event) {
        var all = reviews.findTop20ByEvent_IdOrderByCreatedAtDesc(event.getId());
        if (all.isEmpty()) {
            event.setRating(BigDecimal.ZERO);
        } else {
            var sum = all.stream().map(Review::getRating).mapToInt(Integer::intValue).sum();
            var avg = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(all.size()), 1, RoundingMode.HALF_UP);
            event.setRating(avg);
        }
        events.save(event);
    }
}
