package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    boolean existsByProfileIdAndBadgeId(UUID profileId, UUID badgeId);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.profileId = :profileId")
    Set<UUID> findAwardedBadgeIdsByProfileId(@Param("profileId") UUID profileId);

    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.profileId = :profileId ORDER BY ub.awardedAt DESC")
    List<UserBadge> findByProfileIdWithBadge(@Param("profileId") UUID profileId);

    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.profileId IN (:profileIds)")
    List<UserBadge> findByProfileIdsWithBadge(@Param("profileIds") Collection<UUID> profileIds);
}
