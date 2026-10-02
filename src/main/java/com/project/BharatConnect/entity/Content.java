package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                ),
                @Index(
                        name = "idx_content_parent",
                        columnList = "parent_content_id"
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

    @Column(name = "content_name", length = 150)
    private String contentName;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Builder.Default
    @Column(name = "repost_count", nullable = false)
    private Long repostCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_content_id")
    private Content parentContent;

    @OneToOne(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Poll poll;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContentLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.commentCount == null) {
            this.commentCount = 0L;
        }
        if (this.likeCount == null) {
            this.likeCount = 0L;
        }
        if (this.repostCount == null) {
            this.repostCount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
