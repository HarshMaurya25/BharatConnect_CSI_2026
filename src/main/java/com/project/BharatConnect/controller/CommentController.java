package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.comment.CommentResponseDto;
import com.project.BharatConnect.dto.comment.CommentUpdateRequest;
import com.project.BharatConnect.dto.common.CursorPageResponse;
import com.project.BharatConnect.dto.social.LikeResponseDto;
import com.project.BharatConnect.dto.social.LikerDto;
import com.project.BharatConnect.service.comment.CommentService;
import com.project.BharatConnect.service.social.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final LikeService likeService;

    @PutMapping("/{commentId}/like")
    public ResponseEntity<LikeResponseDto> likeComment(
            @PathVariable UUID commentId
    ) {
        return ResponseEntity.ok(likeService.likeComment(commentId));
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<LikeResponseDto> unlikeComment(
            @PathVariable UUID commentId
    ) {
        return ResponseEntity.ok(likeService.unlikeComment(commentId));
    }

    @GetMapping("/{commentId}/likes")
    public ResponseEntity<Page<LikerDto>> getCommentLikers(
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(likeService.getCommentLikers(commentId, page, size));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<CursorPageResponse<CommentResponseDto>> getReplies(
            @PathVariable UUID commentId
    ) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> editComment(
            @PathVariable UUID commentId,
            @RequestBody @Valid CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.editComment(commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId
    ) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
