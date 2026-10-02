package com.project.BharatConnect.gamification.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyProgressDto {
    private UUID profileId;
    private String userName;
    private String displayName;
    private long totalPoints;
    private int level;
    private String rankTitle;
    private long currentLevelFloor;
    private long nextLevelCeiling;
    private double progressToNextLevel;
    private long pointsToNextLevel;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActiveDate;
    private Long alltimeRank;
    private Long weeklyRank;
    private long weeklyPoints;
    private long badgesCount;
    private List<BadgeDto> topBadges;
    private List<PointTransactionDto> recentTransactions;
    private CountersDto counters;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountersDto {
        private long postsCount;
        private long commentsCount;
        private long likesReceived;
        private long likesGiven;
        private long pollVotes;
        private long quizzesAnswered;
        private long quizzesCorrect;
        private long repostsReceived;
    }
}
