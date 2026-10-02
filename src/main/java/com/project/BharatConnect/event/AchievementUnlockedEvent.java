package com.project.BharatConnect.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AchievementUnlockedEvent {
    private final UUID profileId;
    private final String achievementCode;
    private final String achievementName;
    private final String badgeCode;
    private final Integer rewardPoints;

    public AchievementUnlockedEvent(UUID profileId, String achievementCode) {
        this(profileId, achievementCode, null, null, null);
    }
}
