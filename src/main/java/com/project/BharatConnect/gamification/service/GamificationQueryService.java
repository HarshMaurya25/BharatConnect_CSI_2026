package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.dto.*;
import com.project.BharatConnect.gamification.entity.*;
import com.project.BharatConnect.gamification.repo.*;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationQueryService {

    private final ProfileStatsRepository profileStatsRepository;
    private final WeeklyPointsRepository weeklyPointsRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final BadgeRepository badgeRepository;
    private final AchievementRepository achievementRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ProfileRepository profileRepository;
    private final PointsService pointsService;
    private final GamificationProperties properties;

    @Transactional(readOnly = true)
    public MyProgressDto getMyProgress(UUID profileId) {
        Profile profile = profileRepository.findById(profileId).orElse(null);
        ProfileStats stats = profileStatsRepository.findById(profileId).orElse(null);

        long totalPoints = stats != null ? stats.getTotalPoints() : 0L;
        int level = stats != null ? stats.getLevel() : 1;
        String rankTitle = properties.getRankTitle(level);

        List<Long> thresholds = properties.getLevelThresholds();
        long currentFloor = (level <= 1 || thresholds.isEmpty()) ? 0L : thresholds.get(Math.min(level - 1, thresholds.size() - 1));
        long nextCeiling = level < thresholds.size() ? thresholds.get(level) : (thresholds.isEmpty() ? 100L : thresholds.get(thresholds.size() - 1));
        double progress = nextCeiling > currentFloor
                ? Math.min(1.0, Math.max(0.0, (double) (totalPoints - currentFloor) / (nextCeiling - currentFloor)))
                : 1.0;
        long pointsToNext = Math.max(0L, nextCeiling - totalPoints);

        ZoneId zoneId = ZoneId.of(properties.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);
        String weekKey = pointsService.calculateWeekKey(today);

        Long alltimeRank = profileStatsRepository.calculateAllTimeRank(totalPoints, profileId);
        Optional<WeeklyPoints> wp = weeklyPointsRepository.findByWeekKeyAndProfileId(weekKey, profileId);
        long weeklyPoints = wp.map(WeeklyPoints::getPoints).orElse(0L);
        Long weeklyRank = weeklyPoints > 0 ? weeklyPointsRepository.calculateWeeklyRank(weekKey, weeklyPoints, profileId) : null;

        List<UserBadge> userBadges = userBadgeRepository.findByProfileIdWithBadge(profileId);
        List<BadgeDto> topBadges = userBadges.stream()
                .limit(5)
                .map(ub -> BadgeDto.builder()
                        .id(ub.getBadge().getId())
                        .code(ub.getBadge().getCode())
                        .name(ub.getBadge().getName())
                        .description(ub.getBadge().getDescription())
                        .icon(ub.getBadge().getIcon())
                        .tier(ub.getBadge().getTier())
                        .category(ub.getBadge().getCategory())
                        .hidden(ub.getBadge().isHidden())
                        .unlocked(true)
                        .awardedAt(ub.getAwardedAt())
                        .build())
                .toList();

        List<PointTransaction> recentTxs = pointTransactionRepository.findInitialHistory(profileId, PageRequest.of(0, 10));
        List<PointTransactionDto> txDtos = recentTxs.stream()
                .map(tx -> PointTransactionDto.builder()
                        .id(tx.getId())
                        .action(tx.getAction())
                        .points(tx.getPoints())
                        .sourceType(tx.getSourceType())
                        .sourceId(tx.getSourceId())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();

        MyProgressDto.CountersDto counters = MyProgressDto.CountersDto.builder()
                .postsCount(stats != null ? stats.getPostsCount() : 0L)
                .commentsCount(stats != null ? stats.getCommentsCount() : 0L)
                .likesReceived(stats != null ? stats.getLikesReceived() : 0L)
                .likesGiven(stats != null ? stats.getLikesGiven() : 0L)
                .pollVotes(stats != null ? stats.getPollVotes() : 0L)
                .quizzesAnswered(stats != null ? stats.getQuizzesAnswered() : 0L)
                .quizzesCorrect(stats != null ? stats.getQuizzesCorrect() : 0L)
                .repostsReceived(stats != null ? stats.getRepostsReceived() : 0L)
                .build();

        return MyProgressDto.builder()
                .profileId(profileId)
                .userName(profile != null ? profile.getUserName() : "unknown")
                .displayName(profile != null ? profile.getDisplayName() : "Unknown User")
                .totalPoints(totalPoints)
                .level(level)
                .rankTitle(rankTitle)
                .currentLevelFloor(currentFloor)
                .nextLevelCeiling(nextCeiling)
                .progressToNextLevel(progress)
                .pointsToNextLevel(pointsToNext)
                .currentStreak(stats != null ? stats.getCurrentStreak() : 0)
                .longestStreak(stats != null ? stats.getLongestStreak() : 0)
                .lastActiveDate(stats != null ? stats.getLastActiveDate() : null)
                .alltimeRank(alltimeRank)
                .weeklyRank(weeklyRank)
                .weeklyPoints(weeklyPoints)
                .badgesCount(userBadges.size())
                .topBadges(topBadges)
                .recentTransactions(txDtos)
                .counters(counters)
                .build();
    }

    @Transactional(readOnly = true)
    public PublicProfileGamificationDto getPublicProfileGamification(UUID profileId) {
        Profile profile = profileRepository.findById(profileId).orElse(null);
        ProfileStats stats = profileStatsRepository.findById(profileId).orElse(null);

        long totalPoints = stats != null ? stats.getTotalPoints() : 0L;
        int level = stats != null ? stats.getLevel() : 1;
        String rankTitle = properties.getRankTitle(level);
        Long alltimeRank = profileStatsRepository.calculateAllTimeRank(totalPoints, profileId);

        List<UserBadge> userBadges = userBadgeRepository.findByProfileIdWithBadge(profileId);
        List<BadgeDto> topBadges = userBadges.stream()
                .limit(5)
                .map(ub -> BadgeDto.builder()
                        .id(ub.getBadge().getId())
                        .code(ub.getBadge().getCode())
                        .name(ub.getBadge().getName())
                        .description(ub.getBadge().getDescription())
                        .icon(ub.getBadge().getIcon())
                        .tier(ub.getBadge().getTier())
                        .category(ub.getBadge().getCategory())
                        .hidden(ub.getBadge().isHidden())
                        .unlocked(true)
                        .awardedAt(ub.getAwardedAt())
                        .build())
                .toList();

        return PublicProfileGamificationDto.builder()
                .profileId(profileId)
                .userName(profile != null ? profile.getUserName() : "unknown")
                .displayName(profile != null ? profile.getDisplayName() : "Unknown User")
                .level(level)
                .rankTitle(rankTitle)
                .alltimeRank(alltimeRank)
                .badgesCount(userBadges.size())
                .currentStreak(stats != null ? stats.getCurrentStreak() : 0)
                .topBadges(topBadges)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> getAllBadges(UUID profileId) {
        List<Badge> allBadges = badgeRepository.findByActiveTrueOrderByTierAsc();
        Map<UUID, UserBadge> userBadgeMap = Collections.emptyMap();

        if (profileId != null) {
            userBadgeMap = userBadgeRepository.findByProfileIdWithBadge(profileId).stream()
                    .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub));
        }

        List<BadgeDto> result = new ArrayList<>();
        for (Badge badge : allBadges) {
            UserBadge ub = userBadgeMap.get(badge.getId());
            boolean unlocked = ub != null;

            // If badge is hidden and user hasn't unlocked it, omit from catalog
            if (badge.getHidden() && !unlocked) {
                continue;
            }

            result.add(BadgeDto.builder()
                    .id(badge.getId())
                    .code(badge.getCode())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .icon(badge.getIcon())
                    .tier(badge.getTier())
                    .category(badge.getCategory())
                    .hidden(badge.getHidden())
                    .unlocked(unlocked)
                    .awardedAt(ub != null ? ub.getAwardedAt() : null)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> getAllAchievements(UUID profileId) {
        List<Achievement> achievements = achievementRepository.findAllActiveWithBadge();
        ProfileStats stats = profileId != null ? profileStatsRepository.findById(profileId).orElse(null) : null;
        Set<UUID> awardedBadgeIds = profileId != null ? userBadgeRepository.findAwardedBadgeIdsByProfileId(profileId) : Collections.emptySet();

        List<AchievementDto> result = new ArrayList<>();
        for (Achievement a : achievements) {
            long currentProgress = getMetricValue(stats, a.getMetric());
            boolean unlocked = a.getBadge() != null && awardedBadgeIds.contains(a.getBadge().getId());

            result.add(AchievementDto.builder()
                    .code(a.getCode())
                    .name(a.getName())
                    .description(a.getDescription())
                    .metric(a.getMetric())
                    .progress(Math.min(currentProgress, a.getThreshold()))
                    .threshold(a.getThreshold())
                    .rewardPoints(a.getRewardPoints())
                    .unlocked(unlocked)
                    .badgeCode(a.getBadge() != null ? a.getBadge().getCode() : null)
                    .badgeName(a.getBadge() != null ? a.getBadge().getName() : null)
                    .badgeIcon(a.getBadge() != null ? a.getBadge().getIcon() : null)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public PointsHistoryCursorResponse getPointsHistory(UUID profileId, String cursor, int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        List<PointTransaction> txs;

        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
                String[] parts = decoded.split("::");
                LocalDateTime cursorTime = LocalDateTime.parse(parts[0]);
                UUID cursorId = UUID.fromString(parts[1]);

                txs = pointTransactionRepository.findHistoryWithCursor(profileId, cursorTime, cursorId, PageRequest.of(0, safeLimit + 1));
            } catch (Exception e) {
                log.warn("Invalid cursor format: {}", cursor);
                txs = pointTransactionRepository.findInitialHistory(profileId, PageRequest.of(0, safeLimit + 1));
            }
        } else {
            txs = pointTransactionRepository.findInitialHistory(profileId, PageRequest.of(0, safeLimit + 1));
        }

        boolean hasMore = txs.size() > safeLimit;
        List<PointTransaction> items = hasMore ? txs.subList(0, safeLimit) : txs;

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            PointTransaction last = items.get(items.size() - 1);
            String rawCursor = last.getCreatedAt().toString() + "::" + last.getId().toString();
            nextCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
        }

        List<PointTransactionDto> dtos = items.stream()
                .map(tx -> PointTransactionDto.builder()
                        .id(tx.getId())
                        .action(tx.getAction())
                        .points(tx.getPoints())
                        .sourceType(tx.getSourceType())
                        .sourceId(tx.getSourceId())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();

        return PointsHistoryCursorResponse.builder()
                .items(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private long getMetricValue(ProfileStats stats, AchievementMetric metric) {
        if (stats == null || metric == null) return 0L;
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
