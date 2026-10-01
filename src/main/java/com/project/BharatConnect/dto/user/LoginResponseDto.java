package com.project.BharatConnect.dto.user;

import com.project.BharatConnect.util.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private UUID userId;
    private String email;
    private Role role;
    private String jwtToken;
    private Date expirationDate;
}
