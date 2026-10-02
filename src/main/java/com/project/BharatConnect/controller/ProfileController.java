package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.profile.ProfileCreateRequestDto;
import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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
    public ResponseEntity<ProfileResponseDto> updateProfile(
            @RequestBody @Valid ProfileCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                profileService.updateProfile(requestDto)
        );
    }

    @PostMapping( value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile profileImage
    ) {
        String url = profileService.uploadProfile(profileImage);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponseDto> getProfileById(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                profileService.getProfileById(userId)
        );
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ProfileResponseDto> getProfileByUsername(
            @PathVariable String username
    ) {
        return ResponseEntity.ok(
                profileService.getProfileByUsername(username)
        );
    }

}
