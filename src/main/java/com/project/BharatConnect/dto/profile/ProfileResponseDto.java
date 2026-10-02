package com.project.BharatConnect.dto.profile;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProfileResponseDto {

    private UUID userId;
    private String username;
    private String displayName;
    private String currentStatus;
    private String profileImage;
    private String bio;
    private String organization;

    private Long follower;
    private Long following;
    private Long content;

    private Boolean verified;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
}
