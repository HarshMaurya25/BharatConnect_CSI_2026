package com.project.BharatConnect.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollVoteRequest {
    @NotNull(message = "optionId is required")
    private UUID optionId;
}
