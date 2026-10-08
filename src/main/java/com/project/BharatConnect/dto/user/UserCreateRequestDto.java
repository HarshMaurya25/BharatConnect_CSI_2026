package com.project.BharatConnect.dto.user;

import com.project.BharatConnect.util.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotNull(message = "OTP is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "OTP must be exactly 6 digits"
    )
    private String otp;

    /**
     * Indian mobile number — accepted formats:
     * "+91XXXXXXXXXX" or bare 10 digits starting with 6–9.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+91)?[6-9]\\d{9}$",
            message = "Phone must be a valid Indian mobile number (+91XXXXXXXXXX or 10 digits starting with 6–9)"
    )
    private String phone;

    /**
     * 6-digit OTP delivered to {@link #phone} via WhatsApp / SMS (MiniMoth).
     */
    @NotNull(message = "Phone OTP is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "Phone OTP must be exactly 6 digits"
    )
    private String phoneOtp;
}


