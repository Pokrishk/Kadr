package com.example.Kadr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "logs", indexes = {
        @Index(name = "idx_logs_action", columnList = "action_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "comment_text", columnDefinition = "text")
    private String commentText;

    @OneToMany(mappedBy = "log", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference("log-userLogs")
    private Set<UserLog> userLogs = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
    @Transient
    public String getActorUsernameDisplay() {
        if (userLogs == null || userLogs.isEmpty()) {
            return null;
        }
        return userLogs.stream()
                .map(UserLog::getUser)
                .filter(Objects::nonNull)
                .map(User::getUsername)
                .findFirst()
                .orElse(null);
    }

    @Transient
    public String getTableNameDisplay() {
        if (action == null || action.getTitle() == null) {
            return "";
        }
        String[] parts = action.getTitle().split(":", 2);
        if (parts.length == 2) {
            return parts[1];
        }
        return action.getTitle();
    }

    @Transient
    public String getActionDescriptionDisplay() {
        if (action == null || action.getDescription() == null) {
            return "";
        }
        return action.getDescription();
    }
}
