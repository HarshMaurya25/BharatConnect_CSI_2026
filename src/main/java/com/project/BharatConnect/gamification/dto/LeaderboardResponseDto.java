package com.project.BharatConnect.gamification.dto;

import lombok.*;
import com.project.BharatConnect.gamification.dto.LeaderboardEntryDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardResponseDto {
    private String type;
    private String weekKey;
    private Long myRank;
    private Long myPoints;
    private List<LeaderboardEntryDto> entries;
}
