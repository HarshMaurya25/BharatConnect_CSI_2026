package com.project.BharatConnect.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PollVotedEvent {
    private final UUID contentId;
    private final UUID profileId; // voterId
    private final UUID ownerId;

    public PollVotedEvent(UUID contentId, UUID profileId) {
        this(contentId, profileId, null);
    }
}
