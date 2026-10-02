package com.project.BharatConnect.controller;

import com.project.BharatConnect.service.social.FollowService;
import lombok.RequiredArgsConstructor;
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
}
