package com.project.BharatConnect.dto.social;

import java.util.UUID;

public record UnfollowEvent(
        UUID followerId,
        UUID followingId
) {
}
