package com.project.BharatConnect.service.social;

import com.project.BharatConnect.dto.social.FollowDelta;
import com.project.BharatConnect.dto.social.FollowEvent;
import com.project.BharatConnect.dto.social.UnfollowEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Component
public class FollowEventListener {

    private final AtomicReference<
                ConcurrentHashMap<UUID, FollowDelta>
                > changes = new AtomicReference<>(
            new ConcurrentHashMap<>()
    );

    @EventListener
    public void handleFollow(FollowEvent event) {

        System.out.println("Follow event" + event.followerId() + "AND" + event.followingId());
        // Follower's following count +1
        changes.get()
                .computeIfAbsent(
                        event.followerId(),
                        id -> new FollowDelta()
                )
                .addFollowing(1);

        // Following user's follower count +1
        changes.get()
                .computeIfAbsent(
                        event.followingId(),
                        id -> new FollowDelta()
                )
                .addFollower(1);
    }

    @EventListener
    public void handleUnfollow(UnfollowEvent event) {

        // Follower's following count -1
        changes.get()
                .computeIfAbsent(
                        event.followerId(),
                        id -> new FollowDelta()
                )
                .addFollowing(-1);

        // Following user's follower count -1
        changes.get()
                .computeIfAbsent(
                        event.followingId(),
                        id -> new FollowDelta()
                )
                .addFollower(-1);
    }

    public ConcurrentHashMap<UUID, FollowDelta> swapChanges() {
        return changes.getAndSet(
                new ConcurrentHashMap<>()
        );
    }

}
