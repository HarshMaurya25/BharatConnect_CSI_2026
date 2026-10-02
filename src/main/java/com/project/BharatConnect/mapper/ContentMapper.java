package com.project.BharatConnect.mapper;

import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.PollDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.PollOptionDto;
import com.project.BharatConnect.dto.content.ContentResponseDto.QuotedContentDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.PollRepository;
import com.project.BharatConnect.repo.PollVoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@AllArgsConstructor
public class ContentMapper {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
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
                .quoted(quotedDto)
                .build();
    }
}
