package com.project.BharatConnect.service.social;

import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.dto.social.FollowUserResponseDto;
import com.project.BharatConnect.entity.Follow;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.FollowRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.service.user.UserDetail;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Boolean follow(UUID followingId) {

        UserDetail userDetail =
                (UserDetail) Objects.requireNonNull(
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                ).getPrincipal();

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

        Profile followerProfile = profileRepository.findById(followerId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Follower profile not found")
                );

        Profile followingProfile = profileRepository.findById(followingId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Following profile not found")
                );
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();

        followRepository.save(follow);

        followerProfile.setFollowing(
                followerProfile.getFollowing() + 1
        );

        followingProfile.setFollower(
                followingProfile.getFollower() + 1
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

        Profile followerProfile = profileRepository.findById(followerId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Follower profile not found")
                );

        Profile followingProfile = profileRepository.findById(followingId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Following profile not found")
                );

        followRepository.deleteByFollowerIdAndFollowingId(
                followerId,
                followingId
        );

        followerProfile.setFollowing(
                followerProfile.getFollowing() - 1
        );

        followingProfile.setFollower(
                followingProfile.getFollower() - 1
        );

        return true;
    }

    @Transactional
    public Page<FollowUserResponseDto> getFollowers(
            UUID userId,
            int page,
            int size
    ) {
        return followRepository.findFollowers(
                userId,
                PageRequest.of(page, size)
        );
    }

    @Transactional
    public Page<FollowUserResponseDto> getFollowing(
            UUID userId,
            int page,
            int size
    ) {
        return followRepository.findFollowing(
                userId,
                PageRequest.of(page, size)
        );
    }
}
