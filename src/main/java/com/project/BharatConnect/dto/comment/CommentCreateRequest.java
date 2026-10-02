package com.project.BharatConnect.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {

    @NotBlank(message = "Comment text cannot be blank")
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    private String text;

    private UUID parentCommentId;
}
