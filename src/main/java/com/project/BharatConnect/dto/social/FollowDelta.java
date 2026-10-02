package com.project.BharatConnect.dto.social;

import java.util.concurrent.atomic.AtomicLong;

public class FollowDelta {

    private final AtomicLong deltaFollower = new AtomicLong(0);
    private final AtomicLong deltaFollowing = new AtomicLong(0);

    public void addFollower(long delta) {
        deltaFollower.addAndGet(delta);
    }

    public void addFollowing(long delta) {
        deltaFollowing.addAndGet(delta);
    }

    public long getDeltaFollower() {
        return deltaFollower.get();
    }

    public long getDeltaFollowing() {
        return deltaFollowing.get();
    }
}
