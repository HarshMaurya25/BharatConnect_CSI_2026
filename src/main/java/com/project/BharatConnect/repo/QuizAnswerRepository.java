package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    @Query("SELECT COUNT(a) > 0 FROM QuizAnswer a WHERE a.quiz.id = :quizId AND a.profile.userId = :profileId")
    boolean existsByQuizIdAndProfileUserId(@Param("quizId") UUID quizId, @Param("profileId") UUID profileId);

    @Query("SELECT a FROM QuizAnswer a WHERE a.quiz.id = :quizId AND a.profile.userId = :profileId")
    Optional<QuizAnswer> findByQuizIdAndProfileUserId(@Param("quizId") UUID quizId, @Param("profileId") UUID profileId);

    long countByQuizId(UUID quizId);
}
