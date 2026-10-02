package com.project.BharatConnect.service.ratelimit;

import java.util.UUID;

public interface RateLimiterService {
    void checkLikeRateLimit(UUID profileId);
    void checkCommentRateLimit(UUID profileId);
}
