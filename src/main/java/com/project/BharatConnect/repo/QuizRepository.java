package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByContentId(UUID contentId);

    @Query("""
            SELECT DISTINCT q
            FROM Quiz q
            LEFT JOIN FETCH q.options
            LEFT JOIN FETCH q.content c
            LEFT JOIN FETCH c.profile
            WHERE q.id = :id
            """)
    Optional<Quiz> findByIdWithOptions(
            @Param("id") UUID id
    );

    @Query("""
            SELECT DISTINCT q
            FROM Quiz q
            LEFT JOIN FETCH q.options
            LEFT JOIN FETCH q.content c
            LEFT JOIN FETCH c.profile
            WHERE c.id = :contentId
            """)
    Optional<Quiz> findByContentIdWithOptions(
            @Param("contentId") UUID contentId
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE Quiz q
            SET q.totalAnswers = q.totalAnswers + 1,
                q.correctAnswers =
                    q.correctAnswers +
                    CASE
                        WHEN :isCorrect = true THEN 1
                        ELSE 0
                    END
            WHERE q.id = :id
            """)
    void incrementQuizCounters(
            @Param("id") UUID id,
            @Param("isCorrect") boolean isCorrect
    );
}