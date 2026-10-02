package com.project.BharatConnect.dto.content;

import com.project.BharatConnect.entity.ContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponseDto {

    private UUID id;

    private String username;

    private String displayName;

    private String contentName;

    private String text;

    private ContentType contentType;

    private String contentUrl;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long commentCount = 0L;

    @Builder.Default
    private Long repostCount = 0L;

    private boolean likedByMe;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private PollDto poll;

    private QuotedContentDto quoted;

    public record PollDto(
            UUID pollId,
            LocalDateTime expiresAt,
            boolean expired,
            UUID votedOptionId,
            long totalVotes,
            List<PollOptionDto> options
    ) {}

    public record PollOptionDto(
            UUID id,
            String text,
            long votes
    ) {}

    public record QuotedContentDto(
            UUID id,
            ContentType contentType,
            String text,
            String contentUrl,
            String authorDisplayName,
            String authorUserName,
            LocalDateTime createdAt,
            boolean unavailable
    ) {}
}
