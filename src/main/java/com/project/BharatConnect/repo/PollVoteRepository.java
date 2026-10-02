package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    @Query("SELECT COUNT(v) > 0 FROM PollVote v WHERE v.poll.id = :pollId AND v.profile.userId = :profileId")
    boolean existsByPollIdAndProfileUserId(@Param("pollId") UUID pollId, @Param("profileId") UUID profileId);

    @Query("SELECT v FROM PollVote v WHERE v.poll.id = :pollId AND v.profile.userId = :profileId")
    Optional<PollVote> findByPollIdAndProfileUserId(@Param("pollId") UUID pollId, @Param("profileId") UUID profileId);

    long countByPollId(UUID pollId);
}
