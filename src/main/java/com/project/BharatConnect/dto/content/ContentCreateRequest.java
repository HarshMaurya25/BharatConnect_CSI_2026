package com.project.BharatConnect.dto.content;

import com.project.BharatConnect.entity.ContentType;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentCreateRequest {

    private ContentType contentType;

    private String contentName;

    private String text;

    private List<String> pollOptions;

    private Integer pollDurationHours;

    private UUID parentContentId;
}
