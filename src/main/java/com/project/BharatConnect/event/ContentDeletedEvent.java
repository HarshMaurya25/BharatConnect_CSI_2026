package com.project.BharatConnect.event;

import com.project.BharatConnect.entity.ContentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ContentDeletedEvent {
    private final UUID contentId;
    private final UUID profileId;
    private final ContentType contentType;
}
