package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
