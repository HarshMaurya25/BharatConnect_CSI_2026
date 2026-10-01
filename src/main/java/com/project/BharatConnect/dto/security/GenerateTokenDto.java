package com.project.BharatConnect.dto.security;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateTokenDto {
    private String token;
    private Date expirationDate;
}
