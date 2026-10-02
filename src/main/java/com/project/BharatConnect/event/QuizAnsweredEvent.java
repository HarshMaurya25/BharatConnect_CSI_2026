package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class QuizAnsweredEvent {
    private final UUID contentId;
    private final UUID profileId; // userId/answerer
    private final UUID ownerId;
    private final boolean correct;
}
