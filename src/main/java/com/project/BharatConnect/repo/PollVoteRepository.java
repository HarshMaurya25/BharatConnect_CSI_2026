package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    boolean existsByPollIdAndProfileUserId(UUID pollId, UUID profileId);

    Optional<PollVote> findByPollIdAndProfileUserId(UUID pollId, UUID profileId);

    long countByPollId(UUID pollId);
}
