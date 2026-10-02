package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.PollOption;
import com.project.BharatConnect.entity.PollVote;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.PollOptionRepository;
import com.project.BharatConnect.repo.PollRepository;
import com.project.BharatConnect.repo.PollVoteRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private PollService pollService;

    private Content content;
    private Profile profile;
    private UUID profileId;
    private UUID pollId;
    private UUID optionId1;
    private UUID optionId2;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        profile = Profile.builder().userId(profileId).userName("harsh").displayName("Harsh").build();
        content = Content.builder().id(UUID.randomUUID()).profile(profile).contentType(ContentType.POLL).text("Favorite language?").build();
        pollId = UUID.randomUUID();
        optionId1 = UUID.randomUUID();
        optionId2 = UUID.randomUUID();
    }

    @Test
    void testCreatePoll_WithDuration_Success() {
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Favorite framework?")
                .pollOptions(List.of("Spring Boot", "Quarkus", "Micronaut"))
                .pollDurationHours(24)
                .build();

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Poll saved = pollService.createPoll(content, request);

        assertNotNull(saved);
        assertEquals(content, saved.getContent());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(3, saved.getOptions().size());
        assertEquals("Spring Boot", saved.getOptions().get(0).getOptionText());
        assertEquals(0, saved.getOptions().get(0).getPosition());
        assertEquals(0L, saved.getOptions().get(0).getVoteCount());
        assertEquals("Quarkus", saved.getOptions().get(1).getOptionText());
        assertEquals(1, saved.getOptions().get(1).getPosition());
        verify(pollRepository, times(1)).save(any(Poll.class));
    }

    @Test
    void testCreatePoll_WithoutDuration_Success() {
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Favorite database?")
                .pollOptions(List.of("PostgreSQL", "MySQL"))
                .build();

        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Poll saved = pollService.createPoll(content, request);

        assertNotNull(saved);
        assertNull(saved.getExpiresAt());
        assertEquals(2, saved.getOptions().size());
    }

    @Test
    void testVote_Success_IncrementsVoteCount() {
        Poll poll = Poll.builder()
                .id(pollId)
                .content(content)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        PollOption option = PollOption.builder()
                .id(optionId1)
                .poll(poll)
                .optionText("Option 1")
                .voteCount(5L)
                .build();

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findById(optionId1)).thenReturn(Optional.of(option));
        when(pollVoteRepository.existsByPollIdAndProfileUserId(pollId, profileId)).thenReturn(false);
        when(profileRepository.getReferenceById(profileId)).thenReturn(profile);

        pollService.vote(pollId, optionId1, profileId);

        verify(pollVoteRepository, times(1)).save(any(PollVote.class));
        verify(pollOptionRepository, times(1)).incrementVoteCount(optionId1);
    }

    @Test
    void testVote_PollNotFound_ThrowsContentNotFoundException() {
        when(pollRepository.findById(pollId)).thenReturn(Optional.empty());

        assertThrows(ContentNotFoundException.class, () ->
                pollService.vote(pollId, optionId1, profileId)
        );

        verifyNoInteractions(pollOptionRepository, pollVoteRepository);
    }

    @Test
    void testVote_PollExpired_ThrowsInvalidRequestException() {
        Poll expiredPoll = Poll.builder()
                .id(pollId)
                .content(content)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(expiredPoll));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                pollService.vote(pollId, optionId1, profileId)
        );

        assertEquals("Poll has ended", ex.getMessage());
        verifyNoInteractions(pollOptionRepository, pollVoteRepository);
    }

    @Test
    void testVote_InvalidOption_ThrowsInvalidRequestException() {
        Poll poll = Poll.builder()
                .id(pollId)
                .content(content)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        Poll otherPoll = Poll.builder()
                .id(UUID.randomUUID())
                .build();

        PollOption optionOfOtherPoll = PollOption.builder()
                .id(optionId2)
                .poll(otherPoll)
                .optionText("Other Option")
                .build();

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findById(optionId2)).thenReturn(Optional.of(optionOfOtherPoll));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                pollService.vote(pollId, optionId2, profileId)
        );

        assertEquals("Invalid option", ex.getMessage());
        verify(pollVoteRepository, never()).save(any());
        verify(pollOptionRepository, never()).incrementVoteCount(any());
    }

    @Test
    void testVote_DoubleVote_ThrowsInvalidRequestException() {
        Poll poll = Poll.builder()
                .id(pollId)
                .content(content)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        PollOption option = PollOption.builder()
                .id(optionId1)
                .poll(poll)
                .optionText("Option 1")
                .build();

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findById(optionId1)).thenReturn(Optional.of(option));
        when(pollVoteRepository.existsByPollIdAndProfileUserId(pollId, profileId)).thenReturn(true);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                pollService.vote(pollId, optionId1, profileId)
        );

        assertEquals("Already voted", ex.getMessage());
        verify(pollVoteRepository, never()).save(any());
        verify(pollOptionRepository, never()).incrementVoteCount(any());
    }
}
