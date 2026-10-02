package com.project.BharatConnect.mapper;

import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.entity.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponseDto toResponse(Profile profile) {
        return ProfileResponseDto.builder()
                .userId(profile.getUserId())
                .username(profile.getUserName())
                .displayName(profile.getDisplayName())
                .currentStatus(profile.getCurrentStatus())
                .profileImage(profile.getProfileImage())
                .bio(profile.getBio())
                .organization(profile.getOrganization())
                .follower(profile.getFollower())
                .following(profile.getFollowing())
                .content(profile.getContent())
                .verified(profile.getVerified())
                .createdAt(profile.getCreatedAt())
                .lastUpdatedAt(profile.getLastUpdatedAt())
                .build();
    }
}
