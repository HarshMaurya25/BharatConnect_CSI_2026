package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_profile_like",
                        columnNames = {"comment_id", "profile_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_comment_like_comment",
                        columnList = "comment_id"
                ),
                @Index(
                        name = "idx_comment_like_profile",
                        columnList = "profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

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
