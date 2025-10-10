package com.example.Kadr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "user_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "log_id"})
}, indexes = {
        @Index(name = "idx_user_logs_user", columnList = "user_id"),
        @Index(name = "idx_user_logs_log", columnList = "log_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_log_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private LogEntry log;
}
