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
import com.project.BharatConnect.service.minimoth.PhoneOtpService;
import com.project.BharatConnect.service.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PhoneOtpService phoneOtpService;

    /** Sends an email OTP to the given address. */
    public String sendOtp(String email) {
        return otpService.generateOtp(email);
    }

    /**
     * Sends a WhatsApp/SMS OTP to the given phone number via MiniMoth.
     *
     * @param phone Indian mobile number (+91XXXXXXXXXX or 10 bare digits)
     * @return the otp_id returned by MiniMoth
     */
    public String sendPhoneOtp(String phone) {
        return phoneOtpService.sendOtp(phone);
    }

    @Transactional
    public LoginResponseDto createUser(UserCreateRequestDto requestDto) {

        String email = requestDto.getEmail();
        String phone = requestDto.getPhone();

        // 1. Verify email OTP (existing flow)
        if (!otpService.verifyOtp(email, requestDto.getOtp())) {
            throw new OtpExpireException(email);
        }

        // 2. Verify phone OTP via MiniMoth (WhatsApp/SMS)
        phoneOtpService.verifyOtp(phone, requestDto.getPhoneOtp());

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistException(email);
        }

        User user = User.builder()
                .email(email)
                .phone(phone)
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