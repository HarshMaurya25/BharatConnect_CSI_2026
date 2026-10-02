package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.profile.ProfileCreateRequestDto;
import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<Boolean> createProfileWithUsername(
            @RequestParam String username
    ) {
        return ResponseEntity.ok(
                profileService.createProfileWithUsername(username)
        );
    }

    @PutMapping
    public ResponseEntity<ProfileResponseDto> createFullProfile(
            @RequestBody @Valid ProfileCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                profileService.createFullProfile(requestDto)
        );
    }
}
