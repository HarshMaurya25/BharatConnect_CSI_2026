package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.social.FollowUserResponseDto;
import com.project.BharatConnect.service.social.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{followingId}")
    public ResponseEntity<Boolean> follow(
            @PathVariable UUID followingId
    ) {
        return ResponseEntity.ok(
                followService.follow(followingId)
        );
    }

    @DeleteMapping("/{followingId}")
    public ResponseEntity<Boolean> unfollow(
            @PathVariable UUID followingId
    ) {
        return ResponseEntity.ok(
                followService.unfollow(followingId)
        );
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<FollowUserResponseDto>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                followService.getFollowers(userId, page, size)
        );
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<FollowUserResponseDto>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                followService.getFollowing(userId, page, size)
        );
    }
}
