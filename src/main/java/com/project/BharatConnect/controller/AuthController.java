package com.project.BharatConnect.controller;

import com.project.BharatConnect.dto.user.LoginResponseDto;
import com.project.BharatConnect.dto.user.UserCreateRequestDto;
import com.project.BharatConnect.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<LoginResponseDto> registerUser(
            @RequestBody @Validated UserCreateRequestDto requestDto
    ){
        return ResponseEntity.ok(userService.createUser(requestDto));
    }
}
