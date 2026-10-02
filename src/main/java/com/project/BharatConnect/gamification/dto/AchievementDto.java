package com.project.BharatConnect.gamification.dto;

import com.project.BharatConnect.gamification.entity.AchievementMetric;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDto {
    private String code;
    private String name;
    private String description;
    private AchievementMetric metric;
    private long progress;
    private long threshold;
    private int rewardPoints;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private String badgeCode;
    private String badgeName;
    private String badgeIcon;
}
