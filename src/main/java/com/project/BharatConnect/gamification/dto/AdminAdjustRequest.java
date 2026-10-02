package com.project.BharatConnect.gamification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAdjustRequest {
    @NotNull(message = "Points amount is required")
    private Integer points;

    @NotBlank(message = "Reason is required")
    private String reason;
}
