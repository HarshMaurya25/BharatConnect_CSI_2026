package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "achievements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_achievements_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 30)
    private AchievementMetric metric;

    @Column(name = "threshold", nullable = false)
    private Long threshold;

    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    private Badge badge;

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
