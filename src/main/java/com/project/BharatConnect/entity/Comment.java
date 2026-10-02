package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(
                        name = "idx_comments_content_parent_created_id",
                        columnList = "content_id, parent_comment_id, created_at, id"
                ),
                @Index(
                        name = "idx_comments_parent_created_id",
                        columnList = "parent_comment_id, created_at, id"
                ),
                @Index(
                        name = "idx_comments_profile",
                        columnList = "profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", updatable = false)
    private Comment parentComment;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private long likeCount = 0L;

    @Builder.Default
    @Column(name = "reply_count", nullable = false)
    private long replyCount = 0L;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Builder.Default
    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.parentComment != null && this.parentComment.getParentComment() != null) {
            throw new IllegalStateException("Replies cannot be nested");
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
