package com.project.BharatConnect.dto.content;

import com.project.BharatConnect.entity.Profile;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ContentResponseDto {
    private UUID id;

    private String username;

    private String displayName;

    private String contentName;

    private String text;

    private String contentType;

    private String contentUrl;

    private Long likeCount = 0L;

    private Long commentCount = 0L;

    private LocalDateTime updatedAt;
}
