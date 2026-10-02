package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "daily_quests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily_quests_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 30)
    private QuestMetric metric;

    @Column(name = "target", nullable = false)
    private Integer target;

    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }
}
