package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.WeeklyPoints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyPointsRepository extends JpaRepository<WeeklyPoints, UUID> {

    Optional<WeeklyPoints> findByWeekKeyAndProfileId(String weekKey, UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "INSERT INTO weekly_points (id, week_key, profile_id, points) " +
                    "VALUES (:id, :weekKey, :profileId, GREATEST(0, :delta)) " +
                    "ON CONFLICT (week_key, profile_id) " +
                    "DO UPDATE SET points = GREATEST(0, weekly_points.points + :delta)",
            nativeQuery = true
    )
    void upsertWeeklyPoints(@Param("id") UUID id, @Param("weekKey") String weekKey, @Param("profileId") UUID profileId, @Param("delta") long delta);

    @Query("SELECT wp FROM WeeklyPoints wp WHERE wp.weekKey = :weekKey ORDER BY wp.points DESC, wp.profileId ASC")
    Page<WeeklyPoints> findWeeklyLeaderboard(@Param("weekKey") String weekKey, Pageable pageable);

    @Query("SELECT COUNT(wp) + 1 FROM WeeklyPoints wp WHERE wp.weekKey = :weekKey AND (wp.points > :points OR (wp.points = :points AND wp.profileId < :profileId))")
    long calculateWeeklyRank(@Param("weekKey") String weekKey, @Param("points") long points, @Param("profileId") UUID profileId);

    @Modifying
    @Query("DELETE FROM WeeklyPoints wp WHERE wp.weekKey < :thresholdWeekKey")
    int deleteOlderThanWeekKey(@Param("thresholdWeekKey") String thresholdWeekKey);
}
