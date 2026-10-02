package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CommentDeletedEvent {
    private final UUID commentId;
    private final UUID contentId;
    private final UUID profileId; // author
    private final UUID contentOwnerId;
    private final UUID parentCommentId;
}
