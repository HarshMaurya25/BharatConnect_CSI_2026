package com.project.BharatConnect.dto.comment;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {

    private UUID id;

    private UUID contentId;

    private UUID parentCommentId;

    private String text;

    private String authorDisplayName;

    private String authorUserName;

    private String authorProfileImage;

    private UUID authorProfileId;

    private long likeCount;

    private long replyCount;

    private boolean likedByMe;

    private boolean deleted;

    private boolean edited;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
