package com.project.BharatConnect.gamification.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {
    private long rank;
    private UUID id;
    private String userName;
    private String displayName;
    private long points;
    private int level;
}
