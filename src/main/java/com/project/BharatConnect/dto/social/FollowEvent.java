package com.project.BharatConnect.dto.social;

import java.util.UUID;

public record FollowEvent(
        UUID followerId,
        UUID followingId
) {
}
