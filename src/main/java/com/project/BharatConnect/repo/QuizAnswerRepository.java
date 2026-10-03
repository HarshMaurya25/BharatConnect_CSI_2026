package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    boolean existsByQuizIdAndProfileUserId(
            UUID quizId,
            UUID profileId
    );

    Optional<QuizAnswer> findByQuizIdAndProfileUserId(
            UUID quizId,
            UUID profileId
    );

    long countByQuizId(UUID quizId);
}