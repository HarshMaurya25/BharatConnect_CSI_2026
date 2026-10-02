package com.project.BharatConnect.gamification.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsHistoryCursorResponse {
    private List<PointTransactionDto> items;
    private String nextCursor;
    private boolean hasMore;
}
