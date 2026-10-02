package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.event.LevelUpEvent;
import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.entity.PointAction;
import com.project.BharatConnect.gamification.entity.PointSourceType;
import com.project.BharatConnect.gamification.entity.PointTransaction;
import com.project.BharatConnect.gamification.entity.ProfileStats;
import com.project.BharatConnect.gamification.repo.PointTransactionRepository;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import com.project.BharatConnect.gamification.repo.WeeklyPointsRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsService {

    private final ProfileStatsRepository profileStatsRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final WeeklyPointsRepository weeklyPointsRepository;
    private final ProfileRepository profileRepository;
    private final GamificationProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    private static final Set<PointSourceType> UNCAPPED_SOURCE_TYPES = Set.of(
            PointSourceType.STREAK,
            PointSourceType.QUEST,
            PointSourceType.ACHIEVEMENT,
            PointSourceType.ADMIN
    );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean award(UUID profileId, PointAction action, PointSourceType sourceType, UUID sourceId, int points) {
        if (profileId == null) {
            return false;
        }

        try {
            // Idempotency check if sourceId is provided
            if (sourceId != null && pointTransactionRepository.existsByProfileIdAndActionAndSourceId(profileId, action, sourceId)) {
                log.debug("Points already awarded for profileId={}, action={}, sourceId={}", profileId, action, sourceId);
                return false;
            }

            ProfileStats stats = getOrCreateProfileStats(profileId);
            if (stats == null) {
                log.warn("Cannot award points: Profile {} not found", profileId);
                return false;
            }

            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate today = LocalDate.now(zoneId);
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);

            int finalPoints = points;

            // Apply caps for capped source types
            if (!UNCAPPED_SOURCE_TYPES.contains(sourceType)) {
                // Check action specific caps
                finalPoints = applyActionCap(profileId, action, finalPoints, startOfDay, endOfDay);
                if (finalPoints <= 0) {
                    log.debug("Action cap reached for profileId={}, action={}", profileId, action);
                    return false;
                }

                // Check global daily cap
                int currentDayCappedPoints = pointTransactionRepository.sumCappedPointsToday(profileId, startOfDay, endOfDay, UNCAPPED_SOURCE_TYPES);
                int remainingGlobalCap = Math.max(0, properties.getGlobalDailyCap() - currentDayCappedPoints);
                finalPoints = Math.min(finalPoints, remainingGlobalCap);
                if (finalPoints <= 0) {
                    log.debug("Global daily cap reached for profileId={}", profileId);
                    return false;
                }
            }

            // Insert transaction ledger record
            PointTransaction tx = PointTransaction.builder()
                    .profileId(profileId)
                    .action(action)
                    .points(finalPoints)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            try {
                pointTransactionRepository.saveAndFlush(tx);
            } catch (DataIntegrityViolationException e) {
                log.debug("Concurrent duplicate point award prevented for profileId={}, action={}, sourceId={}", profileId, action, sourceId);
                return false;
            }

            // Update ProfileStats
            long newTotalPoints = Math.max(0L, stats.getTotalPoints() + finalPoints);
            int oldLevel = stats.getLevel();
            int newLevel = calculateLevel(newTotalPoints);

            profileStatsRepository.updatePointsAndLevel(profileId, finalPoints, newLevel);

            // Upsert WeeklyPoints
            String weekKey = calculateWeekKey(today);
            weeklyPointsRepository.upsertWeeklyPoints(UUID.randomUUID(), weekKey, profileId, finalPoints);

            // Publish LevelUpEvent if level increased
            if (newLevel > oldLevel) {
                log.info("Profile {} leveled up from {} to {}", profileId, oldLevel, newLevel);
                eventPublisher.publishEvent(new LevelUpEvent(profileId, oldLevel, newLevel));
            }

            return true;
        } catch (Exception e) {
            log.error("Error awarding points to profile {}: {}", profileId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reversePointsForSource(UUID profileId, UUID sourceId, PointAction reversalAction, PointSourceType sourceType) {
        if (profileId == null || sourceId == null) {
            return false;
        }

        try {
            // Check if already reversed
            if (pointTransactionRepository.existsByProfileIdAndActionAndSourceId(profileId, reversalAction, sourceId)) {
                log.debug("Points already reversed for profileId={}, sourceId={}, action={}", profileId, sourceId, reversalAction);
                return false;
            }

            List<PointTransaction> originalTxs = pointTransactionRepository.findByProfileIdAndSourceId(profileId, sourceId);
            int pointsToDeduct = originalTxs.stream()
                    .filter(tx -> tx.getPoints() > 0 && !tx.getAction().name().endsWith("_REVERSAL") && !tx.getAction().name().endsWith("_REMOVED"))
                    .mapToInt(PointTransaction::getPoints)
                    .sum();

            if (pointsToDeduct <= 0) {
                return false;
            }

            int negativePoints = -pointsToDeduct;

            PointTransaction reversalTx = PointTransaction.builder()
                    .profileId(profileId)
                    .action(reversalAction)
                    .points(negativePoints)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .build();

            pointTransactionRepository.saveAndFlush(reversalTx);

            ProfileStats stats = getOrCreateProfileStats(profileId);
            long newTotalPoints = Math.max(0L, (stats != null ? stats.getTotalPoints() : 0L) + negativePoints);
            int newLevel = calculateLevel(newTotalPoints);

            profileStatsRepository.updatePointsAndLevel(profileId, negativePoints, newLevel);

            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate today = LocalDate.now(zoneId);
            String weekKey = calculateWeekKey(today);
            weeklyPointsRepository.upsertWeeklyPoints(UUID.randomUUID(), weekKey, profileId, negativePoints);

            return true;
        } catch (Exception e) {
            log.error("Error reversing points for profile {} and source {}: {}", profileId, sourceId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean adminAdjust(UUID profileId, int points, String reason) {
        if (profileId == null || points == 0) {
            return false;
        }

        try {
            ProfileStats stats = getOrCreateProfileStats(profileId);
            if (stats == null) {
                return false;
            }

            PointAction action = points > 0 ? PointAction.ADMIN_ADJUST : PointAction.ADMIN_PENALTY;
            PointTransaction tx = PointTransaction.builder()
                    .profileId(profileId)
                    .action(action)
                    .points(points)
                    .sourceType(PointSourceType.ADMIN)
                    .sourceId(UUID.randomUUID())
                    .build();

            pointTransactionRepository.saveAndFlush(tx);

            long newTotalPoints = Math.max(0L, stats.getTotalPoints() + points);
            int oldLevel = stats.getLevel();
            int newLevel = calculateLevel(newTotalPoints);

            profileStatsRepository.updatePointsAndLevel(profileId, points, newLevel);

            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate today = LocalDate.now(zoneId);
            String weekKey = calculateWeekKey(today);
            weeklyPointsRepository.upsertWeeklyPoints(UUID.randomUUID(), weekKey, profileId, points);

            if (newLevel > oldLevel) {
                eventPublisher.publishEvent(new LevelUpEvent(profileId, oldLevel, newLevel));
            }

            return true;
        } catch (Exception e) {
            log.error("Error during admin adjustment for profile {}: {}", profileId, e.getMessage(), e);
            return false;
        }
    }

    public ProfileStats getOrCreateProfileStats(UUID profileId) {
        return profileStatsRepository.findById(profileId).orElseGet(() -> {
            if (!profileRepository.existsById(profileId)) {
                return null;
            }
            try {
                ProfileStats newStats = ProfileStats.builder()
                        .profileId(profileId)
                        .totalPoints(0L)
                        .level(1)
                        .currentStreak(0)
                        .longestStreak(0)
                        .postsCount(0L)
                        .commentsCount(0L)
                        .likesReceived(0L)
                        .likesGiven(0L)
                        .pollVotes(0L)
                        .quizzesAnswered(0L)
                        .quizzesCorrect(0L)
                        .repostsReceived(0L)
                        .build();
                return profileStatsRepository.saveAndFlush(newStats);
            } catch (DataIntegrityViolationException e) {
                return profileStatsRepository.findById(profileId).orElse(null);
            }
        });
    }

    public int calculateLevel(long totalPoints) {
        List<Long> thresholds = properties.getLevelThresholds();
        int level = 1;
        for (int i = 0; i < thresholds.size(); i++) {
            if (totalPoints >= thresholds.get(i)) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    public String calculateWeekKey(LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private int applyActionCap(UUID profileId, PointAction action, int points, LocalDateTime start, LocalDateTime end) {
        Integer cap = null;
        if (action == PointAction.LIKE_RECEIVED_CONTENT || action == PointAction.LIKE_RECEIVED_COMMENT) {
            cap = properties.getLikesReceivedDailyCap();
        } else if (action == PointAction.COMMENT_RECEIVED_CONTENT) {
            cap = properties.getCommentsReceivedDailyCap();
        } else if (action == PointAction.REPOST_RECEIVED_CONTENT) {
            cap = properties.getRepostsReceivedDailyCap();
        }

        if (cap != null) {
            int earnedToday = pointTransactionRepository.sumActionPointsToday(profileId, action, start, end);
            int remaining = Math.max(0, cap - earnedToday);
            return Math.min(points, remaining);
        }

        return points;
    }
}
