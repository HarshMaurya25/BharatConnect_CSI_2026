package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.comment.CommentCreateRequest;
import com.project.BharatConnect.dto.comment.CommentResponseDto;
import com.project.BharatConnect.dto.common.CursorPageResponse;
import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.dto.social.LikeResponseDto;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.service.comment.CommentService;
import com.project.BharatConnect.service.content.ContentService;
import com.project.BharatConnect.service.social.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final LikeService likeService;
    private final CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponseDto> uploadContent(
            @RequestPart(value = "data", required = false) ContentCreateRequest dataReq,
            @RequestPart(value = "content", required = false) ContentCreateRequest contentReq,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        ContentCreateRequest request = dataReq != null ? dataReq : contentReq;
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }
        return ResponseEntity.ok(
                contentService.uploadContent(request, file)
        );
    }

    @PostMapping("/polls/{pollId}/vote/{optionId}")
    public ResponseEntity<Void> votePoll(
            @PathVariable UUID pollId,
            @PathVariable UUID optionId
    ) {
        contentService.votePoll(pollId, optionId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{contentId}/like")
    public ResponseEntity<LikeResponseDto> likeContent(
            @PathVariable UUID contentId
    ) {
        return ResponseEntity.ok(likeService.likeContent(contentId));
    }

    @DeleteMapping("/{contentId}/like")
    public ResponseEntity<LikeResponseDto> unlikeContent(
            @PathVariable UUID contentId
    ) {
        return ResponseEntity.ok(likeService.unlikeContent(contentId));
    }

    @GetMapping("/{contentId}/likes")
    public ResponseEntity<Page<ProfileResponseDto>> getContentLikers(
            @PathVariable UUID contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(likeService.getContentLikers(contentId, page, size));
    }

    @PostMapping("/{contentId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable UUID contentId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(contentId, request));
    }

    @GetMapping("/{contentId}/comments")
    public ResponseEntity<CursorPageResponse<CommentResponseDto>> getTopLevelComments(
            @PathVariable UUID contentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(commentService.getTopLevelComments(contentId, cursor, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ContentResponseDto>> getContentByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                contentService.getContentByUserId(userId, page, size)
        );
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentResponseDto> getContentById(
            @PathVariable UUID contentId
    ) {
        return ResponseEntity.ok(
                contentService.getContentById(contentId)
        );
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Boolean> deleteContent(
            @PathVariable UUID contentId
    ) {
        return ResponseEntity.ok(
                contentService.deleteContent(contentId)
        );
    }
}
