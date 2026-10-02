package com.project.BharatConnect.service.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.*;
import com.project.BharatConnect.mapper.ContentMapper;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.PollRepository;
import com.project.BharatConnect.repo.PollVoteRepository;
import com.project.BharatConnect.repo.QuizAnswerRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentMapperTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAnswerRepository quizAnswerRepository;

    @Mock
    private ContentLikeRepository contentLikeRepository;

    @InjectMocks
    private ContentMapper contentMapper;

    private Profile profile;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        profile = Profile.builder().userId(profileId).userName("harsh").displayName("Harsh M").build();
    }

    @Test
    void testToResponse_PollMapping_WithVotedOption() {
        UUID pollId = UUID.randomUUID();
        UUID optionId1 = UUID.randomUUID();
        UUID optionId2 = UUID.randomUUID();

        Poll poll = Poll.builder()
                .id(pollId)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        PollOption opt1 = PollOption.builder().id(optionId1).poll(poll).optionText("Opt 1").voteCount(3L).position(0).build();
        PollOption opt2 = PollOption.builder().id(optionId2).poll(poll).optionText("Opt 2").voteCount(7L).position(1).build();
        poll.setOptions(List.of(opt1, opt2));

        Content content = Content.builder()
                .id(UUID.randomUUID())
                .profile(profile)
                .contentType(ContentType.POLL)
                .text("Favorite option?")
                .poll(poll)
                .build();

        PollVote vote = PollVote.builder().id(UUID.randomUUID()).poll(poll).option(opt2).profile(profile).build();

        when(pollVoteRepository.findByPollIdAndProfileUserId(pollId, profileId)).thenReturn(Optional.of(vote));

        ContentResponseDto response = contentMapper.toResponse(content, profile.getDisplayName(), profile.getUserName(), profileId);

        assertNotNull(response);
        assertEquals(ContentType.POLL, response.getContentType());
        assertNotNull(response.getPoll());
        assertEquals(pollId, response.getPoll().pollId());
        assertEquals(10L, response.getPoll().totalVotes());
        assertFalse(response.getPoll().expired());
        assertEquals(optionId2, response.getPoll().votedOptionId());
        assertEquals(2, response.getPoll().options().size());
        assertEquals(3L, response.getPoll().options().get(0).votes());
        assertEquals(7L, response.getPoll().options().get(1).votes());
    }

    @Test
    void testToResponse_QuizMapping_HidesAnswersForUnansweredViewer() throws Exception {
        UUID quizId = UUID.randomUUID();
        UUID optId1 = UUID.randomUUID();
        UUID optId2 = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID(); // not creator

        Quiz quiz = Quiz.builder()
                .id(quizId)
                .explanation("This is secret until answered")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .totalAnswers(5L)
                .correctAnswers(3L)
                .build();

        QuizOption opt1 = QuizOption.builder().id(optId1).quiz(quiz).optionText("Delhi").correct(true).pickCount(3L).position(0).build();
        QuizOption opt2 = QuizOption.builder().id(optId2).quiz(quiz).optionText("Mumbai").correct(false).pickCount(2L).position(1).build();
        quiz.setOptions(List.of(opt1, opt2));

        Content content = Content.builder()
                .id(UUID.randomUUID())
                .profile(profile) // creator is profile (profileId != viewerId)
                .contentType(ContentType.QUIZ)
                .text("Capital of India?")
                .quiz(quiz)
                .build();

        when(quizAnswerRepository.findByQuizIdAndProfileUserId(quizId, viewerId)).thenReturn(Optional.empty());

        ContentResponseDto response = contentMapper.toResponse(content, profile.getDisplayName(), profile.getUserName(), viewerId);

        assertNotNull(response);
        assertNotNull(response.getQuiz());
        assertNull(response.getQuiz().explanation(), "Explanation must be hidden");
        assertNull(response.getQuiz().myAnswerOptionId());
        assertNull(response.getQuiz().myAnswerCorrect());
        assertNull(response.getQuiz().options().get(0).correct(), "Correct flag must be null/hidden");
        assertNull(response.getQuiz().options().get(1).correct(), "Correct flag must be null/hidden");

        // Assert JSON does not contain correct flag or explanation
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(response.getQuiz());
        assertFalse(json.contains("\"correct\""), "JSON must NOT leak 'correct' field");
        assertFalse(json.contains("\"explanation\""), "JSON must NOT leak 'explanation' field");
    }

    @Test
    void testToResponse_QuizMapping_ShowsAnswersWhenAnswered() {
        UUID quizId = UUID.randomUUID();
        UUID optId1 = UUID.randomUUID();
        UUID optId2 = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();

        Quiz quiz = Quiz.builder()
                .id(quizId)
                .explanation("Delhi is the capital")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .totalAnswers(5L)
                .correctAnswers(3L)
                .build();

        QuizOption opt1 = QuizOption.builder().id(optId1).quiz(quiz).optionText("Delhi").correct(true).pickCount(3L).position(0).build();
        QuizOption opt2 = QuizOption.builder().id(optId2).quiz(quiz).optionText("Mumbai").correct(false).pickCount(2L).position(1).build();
        quiz.setOptions(List.of(opt1, opt2));

        Content content = Content.builder()
                .id(UUID.randomUUID())
                .profile(profile)
                .contentType(ContentType.QUIZ)
                .text("Capital of India?")
                .quiz(quiz)
                .build();

        QuizAnswer answer = QuizAnswer.builder()
                .id(UUID.randomUUID())
                .quiz(quiz)
                .option(opt1)
                .profile(Profile.builder().userId(viewerId).build())
                .correct(true)
                .build();

        when(quizAnswerRepository.findByQuizIdAndProfileUserId(quizId, viewerId)).thenReturn(Optional.of(answer));

        ContentResponseDto response = contentMapper.toResponse(content, profile.getDisplayName(), profile.getUserName(), viewerId);

        assertNotNull(response);
        assertNotNull(response.getQuiz());
        assertEquals("Delhi is the capital", response.getQuiz().explanation());
        assertEquals(optId1, response.getQuiz().myAnswerOptionId());
        assertEquals(Boolean.TRUE, response.getQuiz().myAnswerCorrect());
        assertEquals(Boolean.TRUE, response.getQuiz().options().get(0).correct());
        assertEquals(Boolean.FALSE, response.getQuiz().options().get(1).correct());
    }

    @Test
    void testToResponse_RepostMapping_ActiveParent() {
        Profile authorProfile = Profile.builder().userId(UUID.randomUUID()).userName("author").displayName("Author Name").build();
        Content parent = Content.builder()
                .id(UUID.randomUUID())
                .profile(authorProfile)
                .contentType(ContentType.TEXT)
                .text("Original post text")
                .createdAt(LocalDateTime.now())
                .build();

        Content repost = Content.builder()
                .id(UUID.randomUUID())
                .profile(profile)
                .contentType(ContentType.REPOST)
                .text("Great point!")
                .parentContent(parent)
                .build();

        ContentResponseDto response = contentMapper.toResponse(repost, profileId);

        assertNotNull(response);
        assertEquals(ContentType.REPOST, response.getContentType());
        assertNotNull(response.getQuoted());
        assertFalse(response.getQuoted().unavailable());
        assertEquals(parent.getId(), response.getQuoted().id());
        assertEquals("Author Name", response.getQuoted().authorDisplayName());
        assertEquals("author", response.getQuoted().authorUserName());
        assertEquals("Original post text", response.getQuoted().text());
    }

    @Test
    void testToResponse_RepostMapping_DeletedParent() {
        Content repost = Content.builder()
                .id(UUID.randomUUID())
                .profile(profile)
                .contentType(ContentType.REPOST)
                .text("Great point!")
                .parentContent(null)
                .build();

        ContentResponseDto response = contentMapper.toResponse(repost, profileId);

        assertNotNull(response);
        assertEquals(ContentType.REPOST, response.getContentType());
        assertNotNull(response.getQuoted());
        assertTrue(response.getQuoted().unavailable());
        assertNull(response.getQuoted().id());
        assertNull(response.getQuoted().text());
    }
}
