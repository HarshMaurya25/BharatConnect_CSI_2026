package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_quest_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_quest_progress_profile_quest_date",
                        columnNames = {"profile_id", "quest_id", "quest_date"}
                )
        },
        indexes = {
                @Index(name = "idx_uqp_profile_date", columnList = "profile_id, quest_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private DailyQuest quest;

    @Column(name = "quest_date", nullable = false)
    private LocalDate questDate;

    @Builder.Default
    @Column(name = "progress", nullable = false)
    private int progress = 0;

    @Builder.Default
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Builder.Default
    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public boolean isCompleted() {
        return completed;
    }

    public boolean getCompleted() {
        return completed;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public boolean getRewardClaimed() {
        return rewardClaimed;
    }
}
