package com.project.BharatConnect.service.ratelimit;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NoOpRateLimiter implements RateLimiterService {

    @Override
    public void checkLikeRateLimit(UUID profileId) {
        // No-op implementation; ready for future Redis/TokenBucket rate limiter
    }

    @Override
    public void checkCommentRateLimit(UUID profileId) {
        // No-op implementation; ready for future Redis/TokenBucket rate limiter
    }
}
