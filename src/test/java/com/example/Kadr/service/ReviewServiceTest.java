package com.example.Kadr.service;

import com.example.Kadr.model.AuditAction;
import com.example.Kadr.model.Event;
import com.example.Kadr.model.Review;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.EventRepository;
import com.example.Kadr.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviews;
    @Mock
    private EventRepository events;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReviewService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addReviewTrimsAndLogs() {
        Event event = Event.builder().id(1L).title("Concert").build();
        User user = User.builder().id(2L).username("alice").build();
        when(events.findById(1L)).thenReturn(Optional.of(event));
        when(reviews.existsByEvent_IdAndUser_Id(1L, 2L)).thenReturn(false);
        when(reviews.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });
        when(reviews.findTop20ByEvent_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(Review.builder().rating(4).build()));

        Review saved = service.addReview(1L, user, 6, "  great  ");

        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getBody()).isEqualTo("great");
        verify(auditLogService).log(eq(AuditAction.CREATE), eq("reviews"), contains("Добавлен отзыв"));
    }

    @Test
    void deleteOwnReviewRemovesWhenOwner() {
        User user = User.builder().id(2L).username("alice").build();
        Event event = Event.builder().id(1L).build();
        Review review = Review.builder().id(3L).user(user).event(event).build();
        when(reviews.findById(3L)).thenReturn(Optional.of(review));
        when(reviews.findTop20ByEvent_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        service.deleteOwnReview(3L, user);

        verify(reviews).delete(review);
        verify(events).save(event);
        verify(auditLogService).log(eq(AuditAction.DELETE), eq("reviews"), contains("Удалён отзыв"));
    }

    @Test
    void addReviewThrowsOnDuplicate() {
        Event event = Event.builder().id(1L).title("Concert").build();
        User user = User.builder().id(2L).username("alice").build();
        when(events.findById(1L)).thenReturn(Optional.of(event));
        when(reviews.existsByEvent_IdAndUser_Id(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.addReview(1L, user, 5, "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}