package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ContentRepostedEvent {
    private final UUID contentId;
    private final UUID reposterId;
    private final UUID ownerId;
}
