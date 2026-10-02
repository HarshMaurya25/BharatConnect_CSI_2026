package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.dto.LeaderboardEntryDto;
import com.project.BharatConnect.gamification.dto.LeaderboardResponseDto;
import com.project.BharatConnect.gamification.entity.ProfileStats;
import com.project.BharatConnect.gamification.entity.WeeklyPoints;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import com.project.BharatConnect.gamification.repo.WeeklyPointsRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final ProfileStatsRepository profileStatsRepository;
    private final WeeklyPointsRepository weeklyPointsRepository;
    private final ProfileRepository profileRepository;
    private final PointsService pointsService;
    private final GamificationProperties properties;

    private static final long CACHE_TTL_MILLIS = 30_000L; // 30 seconds cache

    private final Map<String, CachedLeaderboard> cache = new ConcurrentHashMap<>();

    private record CachedLeaderboard(Instant timestamp, List<LeaderboardEntryDto> entries) {}

    @Transactional(readOnly = true)
    public LeaderboardResponseDto getLeaderboard(String type, int limit, UUID currentUserId) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        boolean isWeekly = "WEEKLY".equalsIgnoreCase(type);

        ZoneId zoneId = ZoneId.of(properties.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);
        String weekKey = isWeekly ? pointsService.calculateWeekKey(today) : null;

        String cacheKey = isWeekly ? "WEEKLY_" + weekKey + "_" + safeLimit : "ALLTIME_" + safeLimit;
        List<LeaderboardEntryDto> entries = getCachedEntries(cacheKey);

        if (entries == null) {
            entries = isWeekly ? fetchWeeklyEntries(weekKey, safeLimit) : fetchAllTimeEntries(safeLimit);
            cache.put(cacheKey, new CachedLeaderboard(Instant.now(), entries));
        }

        Long myRank = null;
        Long myPoints = null;

        if (currentUserId != null) {
            if (isWeekly) {
                Optional<WeeklyPoints> myWp = weeklyPointsRepository.findByWeekKeyAndProfileId(weekKey, currentUserId);
                if (myWp.isPresent()) {
                    myPoints = myWp.get().getPoints();
                    myRank = weeklyPointsRepository.calculateWeeklyRank(weekKey, myPoints, currentUserId);
                }
            } else {
                Optional<ProfileStats> myPs = profileStatsRepository.findById(currentUserId);
                if (myPs.isPresent()) {
                    myPoints = myPs.get().getTotalPoints();
                    myRank = profileStatsRepository.calculateAllTimeRank(myPoints, currentUserId);
                }
            }
        }

        return LeaderboardResponseDto.builder()
                .type(isWeekly ? "WEEKLY" : "ALLTIME")
                .weekKey(weekKey)
                .myRank(myRank)
                .myPoints(myPoints)
                .entries(entries)
                .build();
    }

    private List<LeaderboardEntryDto> getCachedEntries(String cacheKey) {
        CachedLeaderboard cached = cache.get(cacheKey);
        if (cached != null && Instant.now().toEpochMilli() - cached.timestamp().toEpochMilli() < CACHE_TTL_MILLIS) {
            return cached.entries();
        }
        return null;
    }

    private List<LeaderboardEntryDto> fetchAllTimeEntries(int limit) {
        Page<ProfileStats> page = profileStatsRepository.findAllTimeLeaderboard(PageRequest.of(0, limit));
        List<ProfileStats> statsList = page.getContent();
        if (statsList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> profileIds = statsList.stream().map(ProfileStats::getProfileId).collect(Collectors.toSet());
        Map<UUID, Profile> profileMap = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));

        List<LeaderboardEntryDto> result = new ArrayList<>();
        long rank = 1;
        for (ProfileStats ps : statsList) {
            Profile profile = profileMap.get(ps.getProfileId());
            result.add(LeaderboardEntryDto.builder()
                    .rank(rank++)
                    .id(ps.getProfileId())
                    .userName(profile != null ? profile.getUserName() : "unknown")
                    .displayName(profile != null ? profile.getDisplayName() : "Unknown User")
                    .points(ps.getTotalPoints())
                    .level(ps.getLevel())
                    .build());
        }

        return result;
    }

    private List<LeaderboardEntryDto> fetchWeeklyEntries(String weekKey, int limit) {
        Page<WeeklyPoints> page = weeklyPointsRepository.findWeeklyLeaderboard(weekKey, PageRequest.of(0, limit));
        List<WeeklyPoints> wpList = page.getContent();
        if (wpList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> profileIds = wpList.stream().map(WeeklyPoints::getProfileId).collect(Collectors.toSet());
        Map<UUID, Profile> profileMap = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));
        Map<UUID, ProfileStats> statsMap = profileStatsRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(ProfileStats::getProfileId, ps -> ps));

        List<LeaderboardEntryDto> result = new ArrayList<>();
        long rank = 1;
        for (WeeklyPoints wp : wpList) {
            Profile profile = profileMap.get(wp.getProfileId());
            ProfileStats ps = statsMap.get(wp.getProfileId());
            result.add(LeaderboardEntryDto.builder()
                    .rank(rank++)
                    .id(wp.getProfileId())
                    .userName(profile != null ? profile.getUserName() : "unknown")
                    .displayName(profile != null ? profile.getDisplayName() : "Unknown User")
                    .points(wp.getPoints())
                    .level(ps != null ? ps.getLevel() : 1)
                    .build());
        }

        return result;
    }
}
