package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "content_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_content_profile_like",
                        columnNames = {"content_id", "profile_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_content_like_profile_created",
                        columnList = "profile_id, created_at"
                ),
                @Index(
                        name = "idx_content_like_content",
                        columnList = "content_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
