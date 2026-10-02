package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.QuizAnswerResponseDto;
import com.project.BharatConnect.entity.*;
import com.project.BharatConnect.error.exception.ConflictException;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.QuizAnswerRepository;
import com.project.BharatConnect.repo.QuizOptionRepository;
import com.project.BharatConnect.repo.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizOptionRepository quizOptionRepository;

    @Mock
    private QuizAnswerRepository quizAnswerRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private QuizService quizService;

    private Content content;
    private Profile profile;
    private UUID profileId;
    private UUID quizId;
    private UUID optionId1;
    private UUID optionId2;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        profile = Profile.builder().userId(profileId).userName("harsh").displayName("Harsh").build();
        content = Content.builder().id(UUID.randomUUID()).profile(profile).contentType(ContentType.QUIZ).text("Capital of France?").build();
        quizId = UUID.randomUUID();
        optionId1 = UUID.randomUUID();
        optionId2 = UUID.randomUUID();
    }

    @Test
    void testCreateQuiz_Success() {
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.QUIZ)
                .text("Capital of France?")
                .quizOptions(List.of(
                        new ContentCreateRequest.QuizOptionCreateRequest("Paris", true),
                        new ContentCreateRequest.QuizOptionCreateRequest("Lyon", false),
                        new ContentCreateRequest.QuizOptionCreateRequest("Marseille", false)
                ))
                .quizExplanation("Paris is the capital and most populous city of France.")
                .quizDurationHours(48)
                .build();

        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        Quiz saved = quizService.createQuiz(content, request);

        assertNotNull(saved);
        assertEquals(content, saved.getContent());
        assertEquals("Paris is the capital and most populous city of France.", saved.getExplanation());
        assertNotNull(saved.getExpiresAt());
        assertEquals(3, saved.getOptions().size());
        assertEquals("Paris", saved.getOptions().get(0).getOptionText());
        assertTrue(saved.getOptions().get(0).getCorrect());
        assertEquals(0, saved.getOptions().get(0).getPosition());
        assertEquals("Lyon", saved.getOptions().get(1).getOptionText());
        assertFalse(saved.getOptions().get(1).getCorrect());
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void testAnswerQuiz_CorrectAnswer_Success() {
        Quiz quiz = Quiz.builder()
                .id(quizId)
                .content(content)
                .explanation("Paris is capital")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .totalAnswers(0L)
                .correctAnswers(0L)
                .build();

        QuizOption opt1 = QuizOption.builder().id(optionId1).quiz(quiz).optionText("Paris").correct(true).pickCount(0L).build();
        QuizOption opt2 = QuizOption.builder().id(optionId2).quiz(quiz).optionText("Lyon").correct(false).pickCount(0L).build();
        quiz.setOptions(List.of(opt1, opt2));

        when(quizRepository.findByIdWithOptions(quizId)).thenReturn(Optional.of(quiz));
        when(quizAnswerRepository.existsByQuizIdAndProfileUserId(quizId, profileId)).thenReturn(false);
        when(profileRepository.getReferenceById(profileId)).thenReturn(profile);

        QuizAnswerResponseDto response = quizService.answer(quizId, optionId1, profileId);

        assertNotNull(response);
        assertTrue(response.correct());
        assertEquals(optionId1, response.correctOptionId());
        assertEquals("Paris is capital", response.explanation());
        verify(quizAnswerRepository, times(1)).saveAndFlush(any(QuizAnswer.class));
        verify(quizOptionRepository, times(1)).incrementPickCount(optionId1);
        verify(quizRepository, times(1)).incrementQuizCounters(quizId, true);
    }

    @Test
    void testAnswerQuiz_WrongAnswer_Success() {
        Quiz quiz = Quiz.builder()
                .id(quizId)
                .content(content)
                .explanation("Paris is capital")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .totalAnswers(0L)
                .correctAnswers(0L)
                .build();

        QuizOption opt1 = QuizOption.builder().id(optionId1).quiz(quiz).optionText("Paris").correct(true).pickCount(0L).build();
        QuizOption opt2 = QuizOption.builder().id(optionId2).quiz(quiz).optionText("Lyon").correct(false).pickCount(0L).build();
        quiz.setOptions(List.of(opt1, opt2));

        when(quizRepository.findByIdWithOptions(quizId)).thenReturn(Optional.of(quiz));
        when(quizAnswerRepository.existsByQuizIdAndProfileUserId(quizId, profileId)).thenReturn(false);
        when(profileRepository.getReferenceById(profileId)).thenReturn(profile);

        QuizAnswerResponseDto response = quizService.answer(quizId, optionId2, profileId);

        assertNotNull(response);
        assertFalse(response.correct());
        assertEquals(optionId1, response.correctOptionId());
        verify(quizOptionRepository, times(1)).incrementPickCount(optionId2);
        verify(quizRepository, times(1)).incrementQuizCounters(quizId, false);
    }

    @Test
    void testAnswerQuiz_DuplicateAnswer_ThrowsConflictException_409() {
        Quiz quiz = Quiz.builder()
                .id(quizId)
                .content(content)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        QuizOption opt1 = QuizOption.builder().id(optionId1).quiz(quiz).optionText("Paris").correct(true).build();
        quiz.setOptions(List.of(opt1));

        when(quizRepository.findByIdWithOptions(quizId)).thenReturn(Optional.of(quiz));
        when(quizAnswerRepository.existsByQuizIdAndProfileUserId(quizId, profileId)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                quizService.answer(quizId, optionId1, profileId)
        );
        assertEquals("Already answered this quiz", ex.getMessage());
        verify(quizAnswerRepository, never()).saveAndFlush(any());
        verify(quizOptionRepository, never()).incrementPickCount(any());
    }

    @Test
    void testAnswerQuiz_ExpiredQuiz_ThrowsConflictException_409() {
        Quiz quiz = Quiz.builder()
                .id(quizId)
                .content(content)
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(quizRepository.findByIdWithOptions(quizId)).thenReturn(Optional.of(quiz));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                quizService.answer(quizId, optionId1, profileId)
        );
        assertEquals("Quiz has ended", ex.getMessage());
    }

    @Test
    void testAnswerQuiz_InvalidOption_ThrowsInvalidRequestException_400() {
        Quiz quiz = Quiz.builder()
                .id(quizId)
                .content(content)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        QuizOption opt1 = QuizOption.builder().id(optionId1).quiz(quiz).optionText("Paris").correct(true).build();
        quiz.setOptions(List.of(opt1));

        when(quizRepository.findByIdWithOptions(quizId)).thenReturn(Optional.of(quiz));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                quizService.answer(quizId, UUID.randomUUID(), profileId)
        );
        assertEquals("Invalid option", ex.getMessage());
    }
}
