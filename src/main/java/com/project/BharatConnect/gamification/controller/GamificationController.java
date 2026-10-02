package com.project.BharatConnect.gamification.controller;

import com.project.BharatConnect.gamification.dto.*;
import com.project.BharatConnect.gamification.service.*;
import com.project.BharatConnect.service.user.UserDetail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
@Slf4j
public class GamificationController {

    private final GamificationQueryService gamificationQueryService;
    private final QuestService questService;
    private final LeaderboardService leaderboardService;
    private final PointsService pointsService;
    private final BackfillService backfillService;

    @GetMapping("/me")
    public ResponseEntity<MyProgressDto> getMyProgress() {
        UUID profileId = getCurrentProfileId();
        return ResponseEntity.ok(gamificationQueryService.getMyProgress(profileId));
    }

    @GetMapping("/profiles/{userId}")
    public ResponseEntity<PublicProfileGamificationDto> getPublicProfileGamification(@PathVariable UUID userId) {
        return ResponseEntity.ok(gamificationQueryService.getPublicProfileGamification(userId));
    }

    @GetMapping("/badges")
    public ResponseEntity<List<BadgeDto>> getAllBadges() {
        UUID profileId = getOptionalProfileId();
        return ResponseEntity.ok(gamificationQueryService.getAllBadges(profileId));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementDto>> getAllAchievements() {
        UUID profileId = getOptionalProfileId();
        return ResponseEntity.ok(gamificationQueryService.getAllAchievements(profileId));
    }

    @GetMapping("/quests/today")
    public ResponseEntity<List<DailyQuestDto>> getTodayQuests() {
        UUID profileId = getCurrentProfileId();
        return ResponseEntity.ok(questService.getTodayQuests(profileId));
    }

    @GetMapping("/me/points-history")
    public ResponseEntity<PointsHistoryCursorResponse> getPointsHistory(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        UUID profileId = getCurrentProfileId();
        return ResponseEntity.ok(gamificationQueryService.getPointsHistory(profileId, cursor, limit));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponseDto> getLeaderboard(
            @RequestParam(defaultValue = "WEEKLY") String type,
            @RequestParam(defaultValue = "20") int limit
    ) {
        UUID profileId = getOptionalProfileId();
        return ResponseEntity.ok(leaderboardService.getLeaderboard(type, limit, profileId));
    }

    @PostMapping("/admin/adjust/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminAdjustPoints(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminAdjustRequest request
    ) {
        boolean success = pointsService.adminAdjust(userId, request.getPoints(), request.getReason());
        return ResponseEntity.ok(Map.of(
                "success", success,
                "userId", userId,
                "adjustedPoints", request.getPoints(),
                "reason", request.getReason()
        ));
    }

    @PostMapping("/admin/backfill")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> backfillAll() {
        int processed = backfillService.backfillAllProfiles();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "processedProfiles", processed
        ));
    }

    private UUID getCurrentProfileId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetail ud) || ud.getUser() == null) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        return ud.getUser().getUserId();
    }

    private UUID getOptionalProfileId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetail ud && ud.getUser() != null) {
            return ud.getUser().getUserId();
        }
        return null;
    }
}
