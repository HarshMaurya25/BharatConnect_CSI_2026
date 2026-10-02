package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.PollOption;
import com.project.BharatConnect.entity.PollVote;
import com.project.BharatConnect.error.exception.ConflictException;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.PollOptionRepository;
import com.project.BharatConnect.repo.PollRepository;
import com.project.BharatConnect.repo.PollVoteRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final ProfileRepository profileRepository;

    public Poll createPoll(Content content, ContentCreateRequest req) {
        Poll poll = Poll.builder()
                .content(content)
                .expiresAt(req.getPollDurationHours() == null ? null
                        : LocalDateTime.now().plusHours(req.getPollDurationHours()))
                .build();

        int i = 0;
        for (String text : req.getPollOptions()) {
            poll.getOptions().add(
                    PollOption.builder()
                            .poll(poll)
                            .optionText(text.trim())
                            .position(i++)
                            .voteCount(0L)
                            .build()
            );
        }

        return pollRepository.save(poll);
    }

    @Transactional
    public ContentResponseDto.PollDto voteByContentId(UUID contentId, UUID optionId, UUID profileId) {
        Poll poll = pollRepository.findByContentIdWithOptions(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Poll not found for content " + contentId));
        return vote(poll.getId(), optionId, profileId);
    }

    @Transactional
    public ContentResponseDto.PollDto vote(UUID pollId, UUID optionId, UUID profileId) {
        Poll poll = pollRepository.findByIdWithOptions(pollId)
                .orElseThrow(() -> new ContentNotFoundException("Poll not found"));

        if (poll.getExpiresAt() != null && poll.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Poll has ended");
        }

        PollOption option = poll.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Invalid option"));

        if (pollVoteRepository.existsByPollIdAndProfileUserId(pollId, profileId)) {
            throw new ConflictException("Already voted");
        }

        try {
            pollVoteRepository.saveAndFlush(
                    PollVote.builder()
                            .poll(poll)
                            .option(option)
                            .profile(profileRepository.getReferenceById(profileId))
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Already voted");
        }

        pollOptionRepository.incrementVoteCount(optionId);

        Poll updatedPoll = pollRepository.findByIdWithOptions(pollId).orElse(poll);

        long totalVotes = updatedPoll.getOptions() != null
                ? updatedPoll.getOptions().stream().mapToLong(PollOption::getVoteCount).sum()
                : 0L;
        boolean expired = updatedPoll.getExpiresAt() != null && updatedPoll.getExpiresAt().isBefore(LocalDateTime.now());

        List<ContentResponseDto.PollOptionDto> optionDtos = updatedPoll.getOptions() != null
                ? updatedPoll.getOptions().stream()
                .map(o -> new ContentResponseDto.PollOptionDto(o.getId(), o.getOptionText(), o.getVoteCount()))
                .toList()
                : Collections.emptyList();

        return new ContentResponseDto.PollDto(
                updatedPoll.getId(),
                updatedPoll.getExpiresAt(),
                expired,
                optionId,
                totalVotes,
                optionDtos
        );
    }
}
