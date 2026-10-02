package com.project.BharatConnect.gamification.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfileGamificationDto {
    private UUID profileId;
    private String userName;
    private String displayName;
    private int level;
    private String rankTitle;
    private Long alltimeRank;
    private long badgesCount;
    private int currentStreak;
    private List<BadgeDto> topBadges;
}
