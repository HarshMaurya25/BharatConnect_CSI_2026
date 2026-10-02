package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.ProfileStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileStatsRepository extends JpaRepository<ProfileStats, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.totalPoints = GREATEST(0L, ps.totalPoints + :delta), ps.level = :level WHERE ps.profileId = :profileId")
    int updatePointsAndLevel(@Param("profileId") UUID profileId, @Param("delta") long delta, @Param("level") int level);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.currentStreak = :streak, ps.longestStreak = GREATEST(ps.longestStreak, :streak), ps.lastActiveDate = :today WHERE ps.profileId = :profileId AND (ps.lastActiveDate IS NULL OR ps.lastActiveDate < :today)")
    int updateStreakIfDateIsOlder(@Param("profileId") UUID profileId, @Param("streak") int streak, @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.postsCount = ps.postsCount + 1 WHERE ps.profileId = :profileId")
    void incrementPostsCount(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.postsCount = GREATEST(0L, ps.postsCount - 1) WHERE ps.profileId = :profileId")
    void decrementPostsCount(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.commentsCount = ps.commentsCount + 1 WHERE ps.profileId = :profileId")
    void incrementCommentsCount(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.commentsCount = GREATEST(0L, ps.commentsCount - 1) WHERE ps.profileId = :profileId")
    void decrementCommentsCount(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.likesReceived = ps.likesReceived + 1 WHERE ps.profileId = :profileId")
    void incrementLikesReceived(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.likesReceived = GREATEST(0L, ps.likesReceived - 1) WHERE ps.profileId = :profileId")
    void decrementLikesReceived(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.likesGiven = ps.likesGiven + 1 WHERE ps.profileId = :profileId")
    void incrementLikesGiven(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.likesGiven = GREATEST(0L, ps.likesGiven - 1) WHERE ps.profileId = :profileId")
    void decrementLikesGiven(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.pollVotes = ps.pollVotes + 1 WHERE ps.profileId = :profileId")
    void incrementPollVotes(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.quizzesAnswered = ps.quizzesAnswered + 1, ps.quizzesCorrect = ps.quizzesCorrect + CASE WHEN :correct = true THEN 1 ELSE 0 END WHERE ps.profileId = :profileId")
    void incrementQuizzesAnswered(@Param("profileId") UUID profileId, @Param("correct") boolean correct);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.repostsReceived = ps.repostsReceived + 1 WHERE ps.profileId = :profileId")
    void incrementRepostsReceived(@Param("profileId") UUID profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProfileStats ps SET ps.repostsReceived = GREATEST(0L, ps.repostsReceived - 1) WHERE ps.profileId = :profileId")
    void decrementRepostsReceived(@Param("profileId") UUID profileId);

    @Query("SELECT ps FROM ProfileStats ps ORDER BY ps.totalPoints DESC, ps.profileId ASC")
    Page<ProfileStats> findAllTimeLeaderboard(Pageable pageable);

    @Query("SELECT COUNT(ps) + 1 FROM ProfileStats ps WHERE ps.totalPoints > :points OR (ps.totalPoints = :points AND ps.profileId < :profileId)")
    long calculateAllTimeRank(@Param("points") long points, @Param("profileId") UUID profileId);
}
