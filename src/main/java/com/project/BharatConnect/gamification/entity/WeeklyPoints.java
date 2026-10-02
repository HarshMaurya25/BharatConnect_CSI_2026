package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "weekly_points",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_points_week_profile",
                        columnNames = {"week_key", "profile_id"}
                )
        },
        indexes = {
                @Index(name = "idx_weekly_points_rank", columnList = "week_key, points DESC, profile_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "week_key", nullable = false, length = 15)
    private String weekKey;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Builder.Default
    @Column(name = "points", nullable = false)
    private Long points = 0L;
}
