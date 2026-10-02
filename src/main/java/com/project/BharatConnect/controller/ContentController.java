package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.service.content.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ContentResponseDto> uploadContent(
            @RequestPart("content") @Valid ContentCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(
                contentService.uploadContent(request, file)
        );
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
