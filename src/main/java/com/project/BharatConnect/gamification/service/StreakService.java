package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.entity.PointAction;
import com.project.BharatConnect.gamification.entity.PointSourceType;
import com.project.BharatConnect.gamification.entity.ProfileStats;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final ProfileStatsRepository profileStatsRepository;
    private final PointsService pointsService;
    private final GamificationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordActivity(UUID profileId) {
        if (profileId == null) {
            return 0;
        }

        try {
            ProfileStats stats = pointsService.getOrCreateProfileStats(profileId);
            if (stats == null) {
                return 0;
            }

            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate today = LocalDate.now(zoneId);
            LocalDate lastActive = stats.getLastActiveDate();

            if (lastActive != null && lastActive.equals(today)) {
                return stats.getCurrentStreak();
            }

            int newStreak = 1;
            if (lastActive != null && lastActive.equals(today.minusDays(1))) {
                newStreak = stats.getCurrentStreak() + 1;
            }

            int updated = profileStatsRepository.updateStreakIfDateIsOlder(profileId, newStreak, today);
            if (updated > 0) {
                log.info("Updated streak for profile {}: streak={}", profileId, newStreak);

                // Daily streak points (use deterministic source ID per day to avoid duplicate streak points if retried)
                UUID streakDaySourceId = UUID.nameUUIDFromBytes(("streak:" + profileId + ":" + today).getBytes());
                pointsService.award(profileId, PointAction.STREAK_DAILY, PointSourceType.STREAK, streakDaySourceId, properties.getStreakDaily());

                // Milestone bonus
                if (properties.getStreakMilestones().containsKey(newStreak)) {
                    int milestoneBonus = properties.getStreakMilestones().get(newStreak);
                    UUID milestoneSourceId = UUID.nameUUIDFromBytes(("streak_milestone:" + profileId + ":" + newStreak + ":" + today).getBytes());
                    pointsService.award(profileId, PointAction.STREAK_MILESTONE_BONUS, PointSourceType.STREAK, milestoneSourceId, milestoneBonus);
                }

                return newStreak;
            }

            return stats.getCurrentStreak();
        } catch (Exception e) {
            log.error("Error updating streak for profile {}: {}", profileId, e.getMessage(), e);
            return 0;
        }
    }
}
