package com.project.BharatConnect.gamification.dto;

import com.project.BharatConnect.gamification.entity.QuestMetric;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyQuestDto {
    private UUID id;
    private String code;
    private String description;
    private QuestMetric metric;
    private int progress;
    private int target;
    private int rewardPoints;
    private boolean completed;
    private boolean rewardClaimed;
}
