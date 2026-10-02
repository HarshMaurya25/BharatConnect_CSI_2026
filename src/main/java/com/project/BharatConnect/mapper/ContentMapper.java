package com.project.BharatConnect.mapper;

import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.PollDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.PollOptionDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.QuizDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.QuizOptionDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.QuotedContentDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.entity.Quiz;
import com.project.BharatConnect.entity.QuizAnswer;
import com.project.BharatConnect.repo.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class ContentMapper {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentLikeRepository contentLikeRepository;

    public ContentResponseDto toResponse(Content content, String displayName, String username) {
        return toResponse(content, displayName, username, null, false);
    }

    public ContentResponseDto toResponse(Content content, UUID currentUserId) {
        String displayName = content.getProfile() != null ? content.getProfile().getDisplayName() : null;
        String username = content.getProfile() != null ? content.getProfile().getUserName() : null;
        boolean likedByMe = currentUserId != null && content.getId() != null &&
                contentLikeRepository.existsByContentIdAndProfileUserId(content.getId(), currentUserId);
        return toResponse(content, displayName, username, currentUserId, likedByMe);
    }

    public ContentResponseDto toResponse(Content content, UUID currentUserId, boolean likedByMe) {
        String displayName = content.getProfile() != null ? content.getProfile().getDisplayName() : null;
        String username = content.getProfile() != null ? content.getProfile().getUserName() : null;
        return toResponse(content, displayName, username, currentUserId, likedByMe);
    }

    public ContentResponseDto toResponse(Content content, String displayName, String username, UUID currentUserId) {
        return toResponse(content, displayName, username, currentUserId, false);
    }

    public ContentResponseDto toResponse(Content content) {
        return toResponse(content, (UUID) null);
    }

    public ContentResponseDto toResponse(Content content, String displayName, String username, UUID currentUserId, boolean likedByMe) {
        if (content == null) {
            return null;
        }

        PollDto pollDto = null;
        if (content.getContentType() == ContentType.POLL) {
            Poll poll = content.getPoll();
            if (poll == null && content.getId() != null) {
                poll = pollRepository.findByContentIdWithOptions(content.getId()).orElse(null);
            }
            if (poll != null) {
                boolean expired = poll.getExpiresAt() != null && poll.getExpiresAt().isBefore(LocalDateTime.now());
                long totalVotes = poll.getOptions() != null
                        ? poll.getOptions().stream().mapToLong(o -> o.getVoteCount()).sum()
                        : 0L;

                UUID votedOptionId = null;
                if (currentUserId != null && poll.getId() != null) {
                    votedOptionId = pollVoteRepository.findByPollIdAndProfileUserId(poll.getId(), currentUserId)
                            .map(v -> v.getOption().getId())
                            .orElse(null);
                }

                List<PollOptionDto> optionDtos = poll.getOptions() != null
                        ? poll.getOptions().stream()
                        .map(o -> new PollOptionDto(o.getId(), o.getOptionText(), o.getVoteCount()))
                        .toList()
                        : Collections.emptyList();

                pollDto = new PollDto(
                        poll.getId(),
                        poll.getExpiresAt(),
                        expired,
                        votedOptionId,
                        totalVotes,
                        optionDtos
                );
            }
        }

        QuizDto quizDto = null;
        if (content.getContentType() == ContentType.QUIZ) {
            Quiz quiz = content.getQuiz();
            if (quiz == null && content.getId() != null) {
                quiz = quizRepository.findByContentIdWithOptions(content.getId()).orElse(null);
            }
            if (quiz != null) {
                boolean expired = quiz.getExpiresAt() != null && quiz.getExpiresAt().isBefore(LocalDateTime.now());
                boolean isCreator = currentUserId != null && content.getProfile() != null &&
                        currentUserId.equals(content.getProfile().getUserId());

                QuizAnswer myAnswer = null;
                if (currentUserId != null && quiz.getId() != null) {
                    myAnswer = quizAnswerRepository.findByQuizIdAndProfileUserId(quiz.getId(), currentUserId).orElse(null);
                }

                boolean showAnswers = expired || isCreator || myAnswer != null;

                UUID myAnswerOptionId = myAnswer != null ? myAnswer.getOption().getId() : null;
                Boolean myAnswerCorrect = myAnswer != null ? myAnswer.getCorrect() : null;
                String explanation = showAnswers ? quiz.getExplanation() : null;

                List<QuizOptionDto> optionDtos = quiz.getOptions() != null
                        ? quiz.getOptions().stream()
                        .map(o -> new QuizOptionDto(
                                o.getId(),
                                o.getOptionText(),
                                o.getPickCount(),
                                showAnswers ? o.getCorrect() : null
                        ))
                        .toList()
                        : Collections.emptyList();

                quizDto = new QuizDto(
                        quiz.getId(),
                        quiz.getExpiresAt(),
                        expired,
                        explanation,
                        myAnswerOptionId,
                        myAnswerCorrect,
                        quiz.getTotalAnswers() != null ? quiz.getTotalAnswers() : 0L,
                        quiz.getCorrectAnswers() != null ? quiz.getCorrectAnswers() : 0L,
                        optionDtos
                );
            }
        }

        QuotedContentDto quotedDto = null;
        if (content.getContentType() == ContentType.REPOST) {
            Content parent = content.getParentContent();
            if (parent != null) {
                Profile parentProfile = parent.getProfile();
                quotedDto = new QuotedContentDto(
                        parent.getId(),
                        parent.getContentType(),
                        parent.getText(),
                        parent.getContentUrl(),
                        parentProfile != null ? parentProfile.getDisplayName() : null,
                        parentProfile != null ? parentProfile.getUserName() : null,
                        parent.getCreatedAt(),
                        false
                );
            } else {
                quotedDto = new QuotedContentDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true
                );
            }
        }

        return ContentResponseDto.builder()
                .id(content.getId())
                .username(username != null ? username : (content.getProfile() != null ? content.getProfile().getUserName() : null))
                .displayName(displayName != null ? displayName : (content.getProfile() != null ? content.getProfile().getDisplayName() : null))
                .contentName(content.getContentName())
                .text(content.getText())
                .contentType(content.getContentType())
                .contentUrl(content.getContentUrl())
                .likeCount(content.getLikeCount() != null ? content.getLikeCount() : 0L)
                .commentCount(content.getCommentCount() != null ? content.getCommentCount() : 0L)
                .repostCount(content.getRepostCount() != null ? content.getRepostCount() : 0L)
                .likedByMe(likedByMe)
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .poll(pollDto)
                .quiz(quizDto)
                .quoted(quotedDto)
                .build();
    }
}
