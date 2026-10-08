package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.user.LoginRequestDto;
import com.project.BharatConnect.dto.user.LoginResponseDto;
import com.project.BharatConnect.dto.user.UserCreateRequestDto;
import com.project.BharatConnect.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<LoginResponseDto> registerUser(
            @RequestBody @Valid UserCreateRequestDto requestDto
    ) {
        return ResponseEntity.ok(userService.createUser(requestDto));
    }

    /**
     * Step 1a — request an email OTP.
     * Sends a 6-digit code to the given email address.
     */
    @PostMapping("/otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String email
    ) {
        userService.sendOtp(email);
        return ResponseEntity.ok("Email OTP sent successfully");
    }

    /**
     * Step 1b — request a phone OTP via WhatsApp / SMS (MiniMoth).
     * Sends a 6-digit code to the given Indian mobile number.
     *
     * @param phone Indian mobile number (+91XXXXXXXXXX or 10 bare digits)
     */
    @PostMapping("/phone-otp")
    public ResponseEntity<String> sendPhoneOtp(
            @RequestParam
            @NotBlank(message = "Phone number is required")
            @Pattern(
                    regexp = "^(\\+91)?[6-9]\\d{9}$",
                    message = "Phone must be a valid Indian mobile number"
            )
            String phone
    ) {
        userService.sendPhoneOtp(phone);
        return ResponseEntity.ok("Phone OTP sent via WhatsApp/SMS successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto requestDto
    ) {
        LoginResponseDto response = userService.login(requestDto);
        return ResponseEntity.ok(response);
    }
}
