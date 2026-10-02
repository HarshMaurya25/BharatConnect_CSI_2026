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

    @NotNull(message = "Role is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "OTP must be exactly 6 digits"
    )
    private String otp;
}

