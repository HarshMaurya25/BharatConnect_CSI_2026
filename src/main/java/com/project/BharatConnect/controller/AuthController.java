package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.user.LoginRequestDto;
import com.project.BharatConnect.dto.user.LoginResponseDto;
import com.project.BharatConnect.dto.user.UserCreateRequestDto;
import com.project.BharatConnect.service.user.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<LoginResponseDto> registerUser(
            @RequestBody @Valid UserCreateRequestDto requestDto
    ){
        return ResponseEntity.ok(userService.createUser(requestDto));
    }

    @PostMapping("/otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String email
    ) {

        userService.sendOtp(email);

        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto requestDto
    ) {

        LoginResponseDto response = userService.login(requestDto);

        return ResponseEntity.ok(response);
    }
}
