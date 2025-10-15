package com.example.Kadr.service;

import com.example.Kadr.model.Review;
import com.example.Kadr.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<Review> getLatestFiveStar(int limit) {
        return reviewRepository
                .findByRatingOrderByCreatedAtDesc(5, PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .getContent();
    }
    @Transactional(readOnly = true)
    public List<Review> getForEvent(Long eventId, int limit) {
        return reviewRepository
                .findByEvent_IdOrderByCreatedAtDesc(eventId, PageRequest.of(0, limit))
                .getContent();
    }
}
