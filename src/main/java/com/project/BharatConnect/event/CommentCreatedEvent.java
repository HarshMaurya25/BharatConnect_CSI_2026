package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final UUID commentId;
    private final UUID contentId;
    private final UUID profileId;
    private final UUID parentCommentId;
}
