package com.project.BharatConnect.repo;

import com.project.BharatConnect.dto.social.FollowUserResponseDto;
import com.project.BharatConnect.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingId(
            UUID followerId,
            UUID followingId
    );

    long countByFollowerId(UUID followerId);

    long countByFollowingId(UUID followingId);

    void deleteByFollowerIdAndFollowingId(
            UUID followerId,
            UUID followingId
    );

    @Query("""
        SELECT new com.project.BharatConnect.dto.follow.FollowUserResponseDto(
            p.userId,
            p.userName,
            p.displayName
        )
        FROM Follow f
        JOIN Profile p ON p.userId = f.followerId
        WHERE f.followingId = :userId
        """)
    Page<FollowUserResponseDto> findFollowers(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
        SELECT new com.project.BharatConnect.dto.follow.FollowUserResponseDto(
            p.userId,
            p.userName,
            p.displayName
        )
        FROM Follow f
        JOIN Profile p ON p.userId = f.followingId
        WHERE f.followerId = :userId
        """)
    Page<FollowUserResponseDto> findFollowing(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
