package com.project.BharatConnect.service.social;

import com.project.BharatConnect.dto.social.FollowEvent;
import com.project.BharatConnect.dto.social.UnfollowEvent;
import com.project.BharatConnect.entity.Follow;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.FollowRepository;
import com.project.BharatConnect.service.user.UserDetail;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Boolean follow(UUID followingId) {

        UserDetail userDetail =
                (UserDetail) Objects.requireNonNull(
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                ).getPrincipal();

        assert userDetail != null;
        UUID followerId = userDetail.getUser().getUserId();

        if (followerId.equals(followingId)) {
            throw new InvalidRequestException(
                    "You cannot follow yourself"
            );
        }

        if (followRepository.existsByFollowerIdAndFollowingId(
                followerId,
                followingId
        )) {
            throw new InvalidRequestException(
                    "Already following this user"
            );
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();

        followRepository.save(follow);

        eventPublisher.publishEvent(
                new FollowEvent(
                        followerId,
                        followingId
                )
        );

        return true;
    }

    @Transactional
    public Boolean unfollow(UUID followingId) {

        UserDetail userDetail =
                (UserDetail) Objects.requireNonNull(
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                ).getPrincipal();

        assert userDetail != null;

        UUID followerId = userDetail.getUser().getUserId();

        if (followerId.equals(followingId)) {
            throw new InvalidRequestException(
                    "You cannot unfollow yourself"
            );
        }

        if (!followRepository.existsByFollowerIdAndFollowingId(
                followerId,
                followingId
        )) {
            throw new InvalidRequestException(
                    "You are not following this user"
            );
        }

        followRepository.deleteByFollowerIdAndFollowingId(
                followerId,
                followingId
        );

        eventPublisher.publishEvent(
                new UnfollowEvent(
                        followerId,
                        followingId
                )
        );

        return true;
    }
}
