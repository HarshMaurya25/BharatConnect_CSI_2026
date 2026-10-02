package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {

    Optional<Poll> findByContentId(UUID contentId);

    @Query("SELECT p FROM Poll p LEFT JOIN FETCH p.options WHERE p.id = :id")
    Optional<Poll> findByIdWithOptions(@Param("id") UUID id);

    @Query("SELECT p FROM Poll p LEFT JOIN FETCH p.options WHERE p.content.id = :contentId")
    Optional<Poll> findByContentIdWithOptions(@Param("contentId") UUID contentId);
}
