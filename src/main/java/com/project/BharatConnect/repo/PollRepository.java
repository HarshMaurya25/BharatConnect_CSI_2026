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

    @Query("""
        SELECT DISTINCT p
        FROM Poll p
        LEFT JOIN FETCH p.options
        LEFT JOIN FETCH p.content c
        LEFT JOIN FETCH c.profile
        WHERE p.id = :id
        """)
    Optional<Poll> findByIdWithOptions(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT p
        FROM Poll p
        LEFT JOIN FETCH p.options
        LEFT JOIN FETCH p.content c
        LEFT JOIN FETCH c.profile
        WHERE c.id = :contentId
        """)
    Optional<Poll> findByContentIdWithOptions(@Param("contentId") UUID contentId);
}