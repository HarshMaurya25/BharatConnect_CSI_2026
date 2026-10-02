package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CommentUnlikedEvent {
    private final UUID commentId;
    private final UUID profileId; // unlikerId
    private final UUID authorId;
}
