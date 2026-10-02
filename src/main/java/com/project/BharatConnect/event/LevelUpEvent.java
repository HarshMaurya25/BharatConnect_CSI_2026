package com.project.BharatConnect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class LevelUpEvent {
    private final UUID profileId;
    private final int oldLevel;
    private final int newLevel;
}
