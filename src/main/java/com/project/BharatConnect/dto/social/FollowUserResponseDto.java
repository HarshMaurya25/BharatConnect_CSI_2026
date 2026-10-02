package com.project.BharatConnect.dto.social;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class FollowUserResponseDto {

    private UUID userId;
    private String username;
    private String displayName;
}