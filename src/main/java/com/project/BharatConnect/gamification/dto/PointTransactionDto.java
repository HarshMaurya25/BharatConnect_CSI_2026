package com.project.BharatConnect.gamification.dto;

import com.project.BharatConnect.gamification.entity.PointAction;
import com.project.BharatConnect.gamification.entity.PointSourceType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransactionDto {
    private UUID id;
    private PointAction action;
    private int points;
    private PointSourceType sourceType;
    private UUID sourceId;
    private LocalDateTime createdAt;
}
