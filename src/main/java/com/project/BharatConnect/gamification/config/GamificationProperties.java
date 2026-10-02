package com.project.BharatConnect.gamification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.gamification")
@Getter
@Setter
public class GamificationProperties {

    private String timeZone = "Asia/Kolkata";

    // Daily caps
    private int globalDailyCap = 200;
    private int likesReceivedDailyCap = 20;
    private int commentsReceivedDailyCap = 40;
    private int repostsReceivedDailyCap = 30;

    // Content points
    private int postText = 5;
    private int postImage = 10;
    private int postVideo = 15;
    private int postPoll = 8;
    private int postQuiz = 12;
    private int postRepost = 3;

    // Comment points
    private int commentCreate = 3;
    private int replyCreate = 2;

    // Engagement received points
    private int likeReceivedContent = 1;
    private int likeReceivedComment = 1;
    private int commentReceivedContent = 2;
    private int repostReceivedContent = 2;

    // Interactive points
    private int pollVote = 1;
    private int quizAnswer = 1;
    private int quizCorrectBonus = 5;

    // Streak points
    private int streakDaily = 5;
    private Map<Integer, Integer> streakMilestones = Map.of(
            3, 10,
            7, 25,
            14, 50,
            30, 150,
            100, 500
    );

    // First time bonuses
    private int firstPostBonus = 20;
    private int firstCommentBonus = 10;
    private int firstLikeGivenBonus = 5;

    // Level thresholds (cumulative total points)
    private List<Long> levelThresholds = List.of(
            0L, 50L, 150L, 300L, 600L, 1000L, 1600L, 2500L, 4000L, 6000L
    );

    public String getRankTitle(int level) {
        if (level <= 2) return "Newcomer";
        if (level <= 4) return "Explorer";
        if (level <= 6) return "Contributor";
        if (level <= 8) return "Influencer";
        return "Legend";
    }
}
