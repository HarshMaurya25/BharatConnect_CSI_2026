package com.project.BharatConnect.dto.social;

import java.util.UUID;

public record LikerDto(
        UUID id,
        String userName,
        String displayName
) {}
