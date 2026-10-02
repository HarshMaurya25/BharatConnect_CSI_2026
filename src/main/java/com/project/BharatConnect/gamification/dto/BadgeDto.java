package com.project.BharatConnect.gamification.dto;

import com.project.BharatConnect.gamification.entity.BadgeTier;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private BadgeTier tier;
    private String category;
    private boolean hidden;
    private boolean unlocked;
    private LocalDateTime awardedAt;
}
