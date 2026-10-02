package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.event.AchievementUnlockedEvent;
import com.project.BharatConnect.gamification.entity.*;
import com.project.BharatConnect.gamification.repo.AchievementRepository;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import com.project.BharatConnect.gamification.repo.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementEvaluator {

    private final AchievementRepository achievementRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ProfileStatsRepository profileStatsRepository;
    private final PointsService pointsService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluate(UUID profileId, Collection<AchievementMetric> metrics) {
        if (profileId == null || metrics == null || metrics.isEmpty()) {
            return;
        }

        try {
            ProfileStats stats = profileStatsRepository.findById(profileId).orElse(null);
            if (stats == null) {
                return;
            }

            List<Achievement> achievements = achievementRepository.findActiveByMetricsWithBadge(metrics);
            if (achievements.isEmpty()) {
                return;
            }

            Set<UUID> awardedBadgeIds = userBadgeRepository.findAwardedBadgeIdsByProfileId(profileId);

            for (Achievement achievement : achievements) {
                Badge badge = achievement.getBadge();
                if (badge == null || awardedBadgeIds.contains(badge.getId())) {
                    continue;
                }

                long currentValue = getMetricValue(stats, achievement.getMetric());
                if (currentValue >= achievement.getThreshold()) {
                    unlockAchievement(profileId, achievement, badge);
                    awardedBadgeIds.add(badge.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error evaluating achievements for profile {}: {}", profileId, e.getMessage(), e);
        }
    }

    private void unlockAchievement(UUID profileId, Achievement achievement, Badge badge) {
        try {
            UserBadge userBadge = UserBadge.builder()
                    .profileId(profileId)
                    .badge(badge)
                    .awardedAt(LocalDateTime.now())
                    .build();

            userBadgeRepository.saveAndFlush(userBadge);
            log.info("Awarded badge {} ({}) to profile {}", badge.getName(), badge.getCode(), profileId);

            if (achievement.getRewardPoints() > 0) {
                pointsService.award(
                        profileId,
                        PointAction.ACHIEVEMENT_UNLOCKED,
                        PointSourceType.ACHIEVEMENT,
                        achievement.getId(),
                        achievement.getRewardPoints()
                );
            }

            eventPublisher.publishEvent(new AchievementUnlockedEvent(
                    profileId,
                    achievement.getCode(),
                    achievement.getName(),
                    badge.getCode(),
                    achievement.getRewardPoints()
            ));
        } catch (DataIntegrityViolationException e) {
            log.debug("Badge {} already awarded to profile {}", badge.getCode(), profileId);
        } catch (Exception e) {
            log.error("Error unlocking achievement {} for profile {}: {}", achievement.getCode(), profileId, e.getMessage(), e);
        }
    }

    private long getMetricValue(ProfileStats stats, AchievementMetric metric) {
        if (metric == null) return 0L;
        return switch (metric) {
            case POSTS, POSTS_COUNT -> stats.getPostsCount();
            case COMMENTS, COMMENTS_COUNT -> stats.getCommentsCount();
            case LIKES_RECEIVED -> stats.getLikesReceived();
            case LIKES_GIVEN -> stats.getLikesGiven();
            case POLL_VOTES -> stats.getPollVotes();
            case QUIZZES_ANSWERED -> stats.getQuizzesAnswered();
            case QUIZZES_CORRECT -> stats.getQuizzesCorrect();
            case REPOSTS_RECEIVED -> stats.getRepostsReceived();
            case STREAK, STREAK_DAYS -> stats.getCurrentStreak();
            case LONGEST_STREAK -> stats.getLongestStreak();
            case POINTS, TOTAL_POINTS -> stats.getTotalPoints();
            case LEVEL -> stats.getLevel();
            case NIGHT_OWL -> 0L;
        };
    }
}
