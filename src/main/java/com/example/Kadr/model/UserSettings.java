package com.example.Kadr.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_settings_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "theme", nullable = false, length = 16)
    private String theme;

    @Column(name = "font_family", nullable = false, length = 64)
    private String fontFamily;

    @Column(name = "font_size", nullable = false)
    private Integer fontSize;

    @Column(name = "page_size", nullable = false)
    private Integer pageSize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saved_filters", nullable = false, columnDefinition = "jsonb")
    private ObjectNode savedFilters;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (theme == null) {
            theme = "system";
        }
        if (fontFamily == null) {
            fontFamily = "system-ui";
        }
        if (fontSize == null) {
            fontSize = 14;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        if (savedFilters == null) {
            savedFilters = JsonNodeFactory.instance.objectNode();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    public void onUpdate() {
        if (savedFilters == null) {
            savedFilters = JsonNodeFactory.instance.objectNode();
        }
    }
}