package com.project.BharatConnect.dto.security;

import com.project.BharatConnect.util.Role;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JwtTokenDto {
    private String userId;
    private String email;
    private Role role;
    private Date getExpiration;
}
