package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "contents",
        indexes = {
                @Index(
                        name = "idx_content_profile",
                        columnList = "profile_id"
                ),
                @Index(
                        name = "idx_content_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_content_type",
                        columnList = "content_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "profile_id",
            nullable = false
    )
    private Profile profile;

    @Column(name = "content_name", nullable = false, length = 150)
    private String contentName;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.commentCount = 0L;
        this.likeCount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
