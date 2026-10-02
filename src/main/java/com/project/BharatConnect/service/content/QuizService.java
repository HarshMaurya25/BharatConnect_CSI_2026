package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.dto.content.QuizAnswerResponseDto;
import com.project.BharatConnect.entity.*;
import com.project.BharatConnect.error.exception.ConflictException;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.event.QuizAnsweredEvent;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.QuizAnswerRepository;
import com.project.BharatConnect.repo.QuizOptionRepository;
import com.project.BharatConnect.repo.QuizRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Quiz createQuiz(Content content, ContentCreateRequest req) {
        Quiz quiz = Quiz.builder()
                .content(content)
                .explanation(req.getQuizExplanation() != null && !req.getQuizExplanation().isBlank() ? req.getQuizExplanation().trim() : null)
                .expiresAt(req.getQuizDurationHours() == null ? null
                        : LocalDateTime.now().plusHours(req.getQuizDurationHours()))
                .totalAnswers(0L)
                .correctAnswers(0L)
                .build();

        int i = 0;
        for (ContentCreateRequest.QuizOptionCreateRequest optReq : req.getQuizOptions()) {
            quiz.getOptions().add(
                    QuizOption.builder()
                            .quiz(quiz)
                            .optionText(optReq.getText().trim())
                            .position(i++)
                            .correct(Boolean.TRUE.equals(optReq.getCorrect()))
                            .pickCount(0L)
                            .build()
            );
        }

        return quizRepository.save(quiz);
    }

    @Transactional
    public QuizAnswerResponseDto answerByContentId(UUID contentId, UUID optionId, UUID profileId) {
        Quiz quiz = quizRepository.findByContentIdWithOptions(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Quiz not found for content " + contentId));
        return answer(quiz.getId(), optionId, profileId);
    }

    @Transactional
    public QuizAnswerResponseDto answer(UUID quizId, UUID optionId, UUID profileId) {
        Quiz quiz = quizRepository.findByIdWithOptions(quizId)
                .orElseThrow(() -> new ContentNotFoundException("Quiz not found"));

        if (quiz.getExpiresAt() != null && quiz.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Quiz has ended");
        }

        QuizOption chosenOption = quiz.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Invalid option"));

        if (quizAnswerRepository.existsByQuizIdAndProfileUserId(quizId, profileId)) {
            throw new ConflictException("Already answered this quiz");
        }

        boolean isCorrect = Boolean.TRUE.equals(chosenOption.getCorrect());

        try {
            quizAnswerRepository.saveAndFlush(
                    QuizAnswer.builder()
                            .quiz(quiz)
                            .option(chosenOption)
                            .profile(profileRepository.getReferenceById(profileId))
                            .correct(isCorrect)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Already answered this quiz");
        }

        quizOptionRepository.incrementPickCount(optionId);
        quizRepository.incrementQuizCounters(quizId, isCorrect);

        UUID contentId = quiz.getContent() != null ? quiz.getContent().getId() : null;
        UUID ownerId = (quiz.getContent() != null && quiz.getContent().getProfile() != null)
                ? quiz.getContent().getProfile().getUserId()
                : null;
        eventPublisher.publishEvent(new QuizAnsweredEvent(contentId, profileId, ownerId, isCorrect));

        Quiz updatedQuiz = quizRepository.findByIdWithOptions(quizId).orElse(quiz);
        UUID correctOptionId = updatedQuiz.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                .map(QuizOption::getId)
                .findFirst()
                .orElse(null);

        List<ContentResponseDto.QuizOptionDto> optionDtos = updatedQuiz.getOptions().stream()
                .map(o -> new ContentResponseDto.QuizOptionDto(
                        o.getId(),
                        o.getOptionText(),
                        o.getPickCount(),
                        o.getCorrect()
                ))
                .toList();

        return new QuizAnswerResponseDto(
                isCorrect,
                correctOptionId,
                updatedQuiz.getExplanation(),
                updatedQuiz.getTotalAnswers(),
                updatedQuiz.getCorrectAnswers(),
                optionDtos
        );
    }
}
