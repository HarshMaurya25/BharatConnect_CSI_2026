package com.project.BharatConnect.service.user;

import com.project.BharatConnect.dto.security.GenerateTokenDto;
import com.project.BharatConnect.dto.user.LoginRequestDto;
import com.project.BharatConnect.dto.user.LoginResponseDto;
import com.project.BharatConnect.dto.user.UserCreateRequestDto;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.InvalidCredentialsException;
import com.project.BharatConnect.error.exception.OtpExpireException;
import com.project.BharatConnect.error.exception.UserAlreadyExistException;
import com.project.BharatConnect.repo.UserRepository;
import com.project.BharatConnect.service.security.JwtService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public String sendOtp(String email) {

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistException(email);
        }

        return otpService.generateOtp(email);
    }

    @Transactional
    public LoginResponseDto createUser(UserCreateRequestDto requestDto) {

        String email = requestDto.getEmail();

        if (!otpService.verifyOtp(email, requestDto.getOtp())) {
            throw new OtpExpireException(email);
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistException(email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(requestDto.getRole())
                .build();

        userRepository.save(user);

        GenerateTokenDto token = jwtService.generateToken(
                user.getUserId().toString(),
                user.getEmail(),
                user.getRole().toString()
        );

        return LoginResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .jwtToken(token.getToken())
                .expirationDate(token.getExpirationDate())
                .role(user.getRole())
                .build();
    }

    public LoginResponseDto login(LoginRequestDto requestDto) {

        String email = requestDto.getEmail();

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException(email);
        }

        if (!passwordEncoder.matches(
                requestDto.getPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(email);
        }

        GenerateTokenDto token = jwtService.generateToken(
                user.getUserId().toString(),
                user.getEmail(),
                user.getRole().toString()
        );

        return LoginResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .jwtToken(token.getToken())
                .expirationDate(token.getExpirationDate())
                .role(user.getRole())
                .build();
    }
}