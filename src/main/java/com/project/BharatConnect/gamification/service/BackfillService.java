package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.entity.AchievementMetric;
import com.project.BharatConnect.gamification.entity.ProfileStats;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import com.project.BharatConnect.gamification.repo.WeeklyPointsRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackfillService {

    private final ProfileRepository profileRepository;
    private final ProfileStatsRepository profileStatsRepository;
    private final WeeklyPointsRepository weeklyPointsRepository;
    private final PointsService pointsService;
    private final AchievementEvaluator achievementEvaluator;
    private final GamificationProperties properties;

    @Transactional
    public int backfillAllProfiles() {
        int page = 0;
        int pageSize = 100;
        int totalProcessed = 0;

        Page<Profile> profilesPage;
        do {
            profilesPage = profileRepository.findAll(PageRequest.of(page, pageSize));
            for (Profile profile : profilesPage.getContent()) {
                backfillProfile(profile.getUserId());
                totalProcessed++;
            }
            page++;
        } while (profilesPage.hasNext());

        log.info("Backfilled gamification stats for {} profiles", totalProcessed);
        return totalProcessed;
    }

    @Transactional
    public void backfillProfile(UUID profileId) {
        if (profileId == null) return;

        ProfileStats stats = pointsService.getOrCreateProfileStats(profileId);
        if (stats == null) return;

        int newLevel = pointsService.calculateLevel(stats.getTotalPoints());
        if (stats.getLevel() != newLevel) {
            stats.setLevel(newLevel);
            profileStatsRepository.save(stats);
        }

        achievementEvaluator.evaluate(profileId, List.of(
                AchievementMetric.POSTS,
                AchievementMetric.COMMENTS,
                AchievementMetric.LIKES_RECEIVED,
                AchievementMetric.LIKES_GIVEN,
                AchievementMetric.POLL_VOTES,
                AchievementMetric.QUIZZES_ANSWERED,
                AchievementMetric.QUIZZES_CORRECT,
                AchievementMetric.REPOSTS_RECEIVED,
                AchievementMetric.POINTS,
                AchievementMetric.LEVEL
        ));
    }

    @Scheduled(cron = "0 0 3 * * SUN") // Every Sunday at 3:00 AM
    @Transactional
    public void cleanupOldWeeklyPoints() {
        try {
            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate thresholdDate = LocalDate.now(zoneId).minusWeeks(8);
            String thresholdWeekKey = pointsService.calculateWeekKey(thresholdDate);

            int deleted = weeklyPointsRepository.deleteOlderThanWeekKey(thresholdWeekKey);
            log.info("Cleaned up {} weekly_points records older than {}", deleted, thresholdWeekKey);
        } catch (Exception e) {
            log.error("Error cleaning up old weekly points: {}", e.getMessage(), e);
        }
    }
}
