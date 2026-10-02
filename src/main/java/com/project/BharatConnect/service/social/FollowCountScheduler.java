package com.project.BharatConnect.service.social;

import com.project.BharatConnect.dto.social.FollowDelta;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.repo.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FollowCountScheduler {

    private final FollowEventListener followEventListener;
    private final ProfileRepository profileRepository;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void updateFollowCounts() {

        ConcurrentHashMap<UUID, FollowDelta> pending =
                followEventListener.swapChanges();

        if (pending.isEmpty()) {
            return;
        }

        pending.forEach((userId, delta) -> {

            Profile profile = profileRepository
                    .findById(userId)
                    .orElse(null);

            if (profile == null) {
                return;
            }

            System.out.println(userId);

            profile.setFollower(
                    profile.getFollower()
                            + delta.getDeltaFollower()
            );

            profile.setFollowing(
                    profile.getFollowing()
                            + delta.getDeltaFollowing()
            );
        });
    }
}
