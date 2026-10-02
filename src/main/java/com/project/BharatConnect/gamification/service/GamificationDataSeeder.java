package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.gamification.entity.*;
import com.project.BharatConnect.gamification.repo.AchievementRepository;
import com.project.BharatConnect.gamification.repo.BadgeRepository;
import com.project.BharatConnect.gamification.repo.DailyQuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationDataSeeder implements ApplicationRunner {

    private final BadgeRepository badgeRepository;
    private final AchievementRepository achievementRepository;
    private final DailyQuestRepository dailyQuestRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and seeding gamification catalogs (badges, achievements, quests)...");
        seedBadgesAndAchievements();
        seedDailyQuests();
        log.info("Gamification catalog seeding completed.");
    }

    private void seedBadgesAndAchievements() {
        // Content badges & achievements
        seedItem("FIRST_POST", "Pioneer", "Published your very first post", "🚀", BadgeTier.BRONZE, "CONTENT",
                "ACH_FIRST_POST", "First Post", AchievementMetric.POSTS, 1, 10);

        seedItem("POST_10", "Author", "Published 10 posts", "✍️", BadgeTier.BRONZE, "CONTENT",
                "ACH_POST_10", "Prolific Writer", AchievementMetric.POSTS, 10, 25);

        seedItem("POST_50", "Prolific Creator", "Published 50 posts", "📚", BadgeTier.SILVER, "CONTENT",
                "ACH_POST_50", "Content Creator", AchievementMetric.POSTS, 50, 50);

        seedItem("POST_100", "Master Storyteller", "Published 100 posts", "👑", BadgeTier.GOLD, "CONTENT",
                "ACH_POST_100", "Master Creator", AchievementMetric.POSTS, 100, 100);

        // Social comments
        seedItem("FIRST_COMMENT", "First Voice", "Left your first comment", "💬", BadgeTier.BRONZE, "SOCIAL",
                "ACH_FIRST_COMMENT", "Conversation Starter", AchievementMetric.COMMENTS, 1, 10);

        seedItem("COMMENT_50", "Active Discussant", "Left 50 comments", "🗣️", BadgeTier.SILVER, "SOCIAL",
                "ACH_COMMENT_50", "Engaged Citizen", AchievementMetric.COMMENTS, 50, 30);

        seedItem("COMMENT_200", "Community Voice", "Left 200 comments", "📢", BadgeTier.GOLD, "SOCIAL",
                "ACH_COMMENT_200", "Community Leader", AchievementMetric.COMMENTS, 200, 75);

        // Likes received
        seedItem("LIKES_REC_50", "Crowd Favorite", "Received 50 likes on your posts", "❤️", BadgeTier.BRONZE, "SOCIAL",
                "ACH_LIKES_REC_50", "Popular Voice", AchievementMetric.LIKES_RECEIVED, 50, 25);

        seedItem("LIKES_REC_500", "Viral Sensation", "Received 500 likes on your posts", "🔥", BadgeTier.SILVER, "SOCIAL",
                "ACH_LIKES_REC_500", "Viral Hit", AchievementMetric.LIKES_RECEIVED, 500, 75);

        seedItem("LIKES_REC_2000", "Superstar", "Received 2000 likes", "🌟", BadgeTier.GOLD, "SOCIAL",
                "ACH_LIKES_REC_2000", "Bharat Superstar", AchievementMetric.LIKES_RECEIVED, 2000, 150);

        // Likes given
        seedItem("LIKES_GIVEN_100", "Super Supporter", "Gave 100 likes to creators", "🤝", BadgeTier.BRONZE, "SOCIAL",
                "ACH_LIKES_GIVEN_100", "Supportive Friend", AchievementMetric.LIKES_GIVEN, 100, 25);

        // Polls
        seedItem("POLL_VOTER_10", "Active Citizen", "Voted in 10 polls", "🗳️", BadgeTier.BRONZE, "ENGAGEMENT",
                "ACH_POLL_10", "Democracy Participant", AchievementMetric.POLL_VOTES, 10, 15);

        seedItem("POLL_VOTER_50", "Democracy Champion", "Voted in 50 polls", "🏛️", BadgeTier.SILVER, "ENGAGEMENT",
                "ACH_POLL_50", "Polling Veteran", AchievementMetric.POLL_VOTES, 50, 40);

        // Quizzes
        seedItem("QUIZ_MASTER_10", "Quiz Whiz", "Answered 10 quizzes correctly", "🧠", BadgeTier.BRONZE, "KNOWLEDGE",
                "ACH_QUIZ_10", "Smart Thinker", AchievementMetric.QUIZZES_CORRECT, 10, 25);

        seedItem("QUIZ_MASTER_50", "Trivia Genius", "Answered 50 quizzes correctly", "🎓", BadgeTier.SILVER, "KNOWLEDGE",
                "ACH_QUIZ_50", "Scholar", AchievementMetric.QUIZZES_CORRECT, 50, 75);

        // Reposts received
        seedItem("REPOST_REC_20", "Trendsetter", "Your posts were reposted 20 times", "🔄", BadgeTier.SILVER, "INFLUENCE",
                "ACH_REPOST_20", "Trend Maker", AchievementMetric.REPOSTS_RECEIVED, 20, 50);

        // Streaks
        seedItem("STREAK_3", "Habit Starter", "Maintained a 3-day active streak", "⚡", BadgeTier.BRONZE, "STREAK",
                "ACH_STREAK_3", "3-Day Streak", AchievementMetric.STREAK, 3, 15);

        seedItem("STREAK_7", "Week Warrior", "Maintained a 7-day active streak", "🔥", BadgeTier.SILVER, "STREAK",
                "ACH_STREAK_7", "Weekly Dedication", AchievementMetric.STREAK, 7, 30);

        seedItem("STREAK_30", "Monthly Maestro", "Maintained a 30-day active streak", "💎", BadgeTier.GOLD, "STREAK",
                "ACH_STREAK_30", "Monthly Devotion", AchievementMetric.STREAK, 30, 100);

        seedItem("STREAK_100", "Centurion", "Maintained a 100-day active streak", "🏆", BadgeTier.PLATINUM, "STREAK",
                "ACH_STREAK_100", "100-Day Legend", AchievementMetric.STREAK, 100, 300);

        // Levels
        seedItem("LEVEL_5", "Rising Star", "Reached Level 5", "⭐", BadgeTier.SILVER, "LEVEL",
                "ACH_LEVEL_5", "Level 5 Achiever", AchievementMetric.LEVEL, 5, 50);

        seedItem("LEVEL_10", "Legend of Bharat", "Reached Level 10", "👑", BadgeTier.PLATINUM, "LEVEL",
                "ACH_LEVEL_10", "Bharat Legend", AchievementMetric.LEVEL, 10, 250);

        // Total Points
        seedItem("POINTS_1000", "Millennium Club", "Accumulated 1,000 total points", "💰", BadgeTier.SILVER, "POINTS",
                "ACH_POINTS_1000", "1K Points", AchievementMetric.POINTS, 1000, 50);

        seedItem("POINTS_5000", "Point Magnate", "Accumulated 5,000 total points", "🔮", BadgeTier.GOLD, "POINTS",
                "ACH_POINTS_5000", "5K Points", AchievementMetric.POINTS, 5000, 150);
    }

    private void seedItem(String badgeCode, String badgeName, String badgeDesc, String badgeIcon,
                          BadgeTier tier, String category,
                          String achCode, String achName, AchievementMetric metric, long threshold, int rewardPoints) {
        Badge badge = badgeRepository.findByCode(badgeCode).orElseGet(() -> {
            Badge b = Badge.builder()
                    .code(badgeCode)
                    .name(badgeName)
                    .description(badgeDesc)
                    .icon(badgeIcon)
                    .tier(tier)
                    .category(category)
                    .hidden(false)
                    .active(true)
                    .build();
            return badgeRepository.save(b);
        });

        achievementRepository.findByCode(achCode).orElseGet(() -> {
            Achievement a = Achievement.builder()
                    .code(achCode)
                    .name(achName)
                    .description(badgeDesc)
                    .badge(badge)
                    .metric(metric)
                    .threshold(threshold)
                    .rewardPoints(rewardPoints)
                    .active(true)
                    .build();
            return achievementRepository.save(a);
        });
    }

    private void seedDailyQuests() {
        seedQuest("DAILY_POST_1", "Publish a new post today", QuestMetric.POSTS_TODAY, 1, 10);
        seedQuest("DAILY_COMMENT_2", "Leave 2 thoughtful comments on posts", QuestMetric.COMMENTS_TODAY, 2, 10);
        seedQuest("DAILY_LIKE_3", "Like 3 posts or comments", QuestMetric.LIKES_GIVEN_TODAY, 3, 5);
        seedQuest("DAILY_POLL_1", "Participate in a poll today", QuestMetric.POLL_VOTES_TODAY, 1, 5);
        seedQuest("DAILY_QUIZ_1", "Answer a quiz today", QuestMetric.QUIZ_ANSWERS_TODAY, 1, 10);
    }

    private void seedQuest(String code, String desc, QuestMetric metric, int target, int reward) {
        dailyQuestRepository.findByCode(code).orElseGet(() -> {
            DailyQuest q = DailyQuest.builder()
                    .code(code)
                    .description(desc)
                    .metric(metric)
                    .target(target)
                    .rewardPoints(reward)
                    .active(true)
                    .build();
            return dailyQuestRepository.save(q);
        });
    }
}
