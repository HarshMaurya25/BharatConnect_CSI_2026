package com.project.BharatConnect.service.user;

import com.project.BharatConnect.dto.security.GenerateTokenDto;
import com.project.BharatConnect.dto.user.LoginResponseDto;
import com.project.BharatConnect.dto.user.UserCreateRequestDto;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.repo.UserRepository;
import com.project.BharatConnect.service.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponseDto createUser(UserCreateRequestDto requestDto){
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(requestDto.getRole())
                .build();

        userRepository.save(user);

        GenerateTokenDto token = jwtService.generateToken(
                user.getUserId().toString(),
                user.getEmail(),
                user.getRole().toString()
        );

        return LoginResponseDto
                .builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .jwtToken(token.getToken())
                .expirationDate(token.getExpirationDate())
                .role(user.getRole())
                .build();
    }
}
