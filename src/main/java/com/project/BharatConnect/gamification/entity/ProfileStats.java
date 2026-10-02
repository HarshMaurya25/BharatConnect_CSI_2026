package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "profile_stats",
        indexes = {
                @Index(name = "idx_profile_stats_points", columnList = "total_points DESC, profile_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileStats {

    @Id
    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private Long totalPoints = 0L;

    @Builder.Default
    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "longest_streak", nullable = false)
    private Integer longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    // Metric counters used by achievements
    @Builder.Default
    @Column(name = "posts_count", nullable = false)
    private Long postsCount = 0L;

    @Builder.Default
    @Column(name = "comments_count", nullable = false)
    private Long commentsCount = 0L;

    @Builder.Default
    @Column(name = "likes_received", nullable = false)
    private Long likesReceived = 0L;

    @Builder.Default
    @Column(name = "likes_given", nullable = false)
    private Long likesGiven = 0L;

    @Builder.Default
    @Column(name = "poll_votes", nullable = false)
    private Long pollVotes = 0L;

    @Builder.Default
    @Column(name = "quizzes_answered", nullable = false)
    private Long quizzesAnswered = 0L;

    @Builder.Default
    @Column(name = "quizzes_correct", nullable = false)
    private Long quizzesCorrect = 0L;

    @Builder.Default
    @Column(name = "reposts_received", nullable = false)
    private Long repostsReceived = 0L;
}
