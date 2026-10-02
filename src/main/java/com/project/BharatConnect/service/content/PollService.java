package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.PollOption;
import com.project.BharatConnect.entity.PollVote;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.repo.PollOptionRepository;
import com.project.BharatConnect.repo.PollRepository;
import com.project.BharatConnect.repo.PollVoteRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void vote(UUID pollId, UUID optionId, UUID profileId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ContentNotFoundException("Poll not found"));

        if (poll.getExpiresAt() != null && poll.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Poll has ended");
        }

        PollOption option = pollOptionRepository.findById(optionId)
                .filter(o -> o.getPoll().getId().equals(pollId))
                .orElseThrow(() -> new InvalidRequestException("Invalid option"));

        if (pollVoteRepository.existsByPollIdAndProfileUserId(pollId, profileId)) {
            throw new InvalidRequestException("Already voted");
        }

        pollVoteRepository.save(
                PollVote.builder()
                        .poll(poll)
                        .option(option)
                        .profile(profileRepository.getReferenceById(profileId))
                        .build()
        );

        pollOptionRepository.incrementVoteCount(optionId);
    }
}
