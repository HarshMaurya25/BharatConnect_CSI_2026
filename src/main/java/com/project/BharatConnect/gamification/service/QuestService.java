package com.project.BharatConnect.gamification.service;

import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.dto.DailyQuestDto;
import com.project.BharatConnect.gamification.entity.*;
import com.project.BharatConnect.gamification.repo.DailyQuestRepository;
import com.project.BharatConnect.gamification.repo.UserQuestProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestService {

    private final DailyQuestRepository dailyQuestRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final PointsService pointsService;
    private final GamificationProperties properties;

    @Transactional(readOnly = true)
    public List<DailyQuestDto> getTodayQuests(UUID profileId) {
        ZoneId zoneId = ZoneId.of(properties.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);
        return getQuestsForDate(profileId, today);
    }

    @Transactional
    public List<DailyQuestDto> getQuestsForDate(UUID profileId, LocalDate date) {
        List<DailyQuest> activeQuests = dailyQuestRepository.findByActiveTrue();
        if (activeQuests.isEmpty()) {
            return Collections.emptyList();
        }

        List<DailyQuest> selectedQuests = selectDailyQuestsForUser(activeQuests, profileId, date, 3);
        List<UUID> questIds = selectedQuests.stream().map(DailyQuest::getId).toList();

        Map<UUID, UserQuestProgress> progressMap = userQuestProgressRepository
                .findByProfileIdAndQuestDateAndQuestIds(profileId, date, questIds)
                .stream()
                .collect(Collectors.toMap(uqp -> uqp.getQuest().getId(), uqp -> uqp));

        List<DailyQuestDto> result = new ArrayList<>();
        for (DailyQuest quest : selectedQuests) {
            UserQuestProgress progress = progressMap.get(quest.getId());
            int currentProgress = progress != null ? progress.getProgress() : 0;
            boolean completed = progress != null && progress.isCompleted();
            boolean rewardClaimed = progress != null && progress.isRewardClaimed();

            result.add(DailyQuestDto.builder()
                    .id(quest.getId())
                    .code(quest.getCode())
                    .description(quest.getDescription())
                    .metric(quest.getMetric())
                    .progress(currentProgress)
                    .target(quest.getTarget())
                    .rewardPoints(quest.getRewardPoints())
                    .completed(completed)
                    .rewardClaimed(rewardClaimed)
                    .build());
        }

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordQuestProgress(UUID profileId, QuestMetric metric, int delta) {
        if (profileId == null || metric == null || delta <= 0) {
            return;
        }

        try {
            ZoneId zoneId = ZoneId.of(properties.getTimeZone());
            LocalDate today = LocalDate.now(zoneId);

            List<DailyQuest> activeQuests = dailyQuestRepository.findByActiveTrue();
            List<DailyQuest> userQuests = selectDailyQuestsForUser(activeQuests, profileId, today, 3);

            for (DailyQuest quest : userQuests) {
                if (quest.getMetric() == metric) {
                    UserQuestProgress progress = userQuestProgressRepository
                            .findByProfileIdAndQuestDateWithQuest(profileId, today)
                            .stream()
                            .filter(p -> p.getQuest().getId().equals(quest.getId()))
                            .findFirst()
                            .orElseGet(() -> UserQuestProgress.builder()
                                    .profileId(profileId)
                                    .quest(quest)
                                    .questDate(today)
                                    .progress(0)
                                    .completed(false)
                                    .rewardClaimed(false)
                                    .build());

                    if (progress.isCompleted()) {
                        continue;
                    }

                    int newProgress = progress.getProgress() + delta;
                    progress.setProgress(newProgress);

                    if (newProgress >= quest.getTarget()) {
                        progress.setCompleted(true);
                        progress.setCompletedAt(LocalDateTime.now());
                        progress.setRewardClaimed(true);

                        userQuestProgressRepository.saveAndFlush(progress);

                        // Award quest completion points
                        pointsService.award(
                                profileId,
                                PointAction.DAILY_QUEST_COMPLETED,
                                PointSourceType.QUEST,
                                progress.getId(),
                                quest.getRewardPoints()
                        );
                        log.info("Quest {} completed by profile {}", quest.getCode(), profileId);
                    } else {
                        userQuestProgressRepository.saveAndFlush(progress);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error recording quest progress for profile {}: {}", profileId, e.getMessage(), e);
        }
    }

    public List<DailyQuest> selectDailyQuestsForUser(List<DailyQuest> allQuests, UUID profileId, LocalDate date, int count) {
        if (allQuests.size() <= count) {
            return allQuests;
        }

        // Deterministic pseudo-random shuffle per user per day
        long seed = (profileId != null ? profileId.getMostSignificantBits() : 0L) ^ date.toEpochDay();
        List<DailyQuest> shuffled = new ArrayList<>(allQuests);
        shuffled.sort(Comparator.comparing(DailyQuest::getCode));
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
