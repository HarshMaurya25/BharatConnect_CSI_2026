package com.project.BharatConnect.service.security;

import com.project.BharatConnect.dto.security.GenerateTokenDto;
import com.project.BharatConnect.dto.security.JwtTokenDto;
import com.project.BharatConnect.error.exception.JwtIllegalTokenException;
import com.project.BharatConnect.util.Role;
import com.project.BharatConnect.util.TokenField;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public GenerateTokenDto generateToken(String subject , String email , String roles){
        Map<String , String> map = new HashMap<>();
        map.put(TokenField.EMAIL.toString() , email);
        map.put(TokenField.ROLE.toString() , roles);

        log.trace("Jwts Token is created with subject Id : {} ({})", subject , email);
        String token = Jwts
                .builder()
                .claims(map)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 259_200_000))
                .signWith(getKey())
                .compact();

        return GenerateTokenDto.builder()
                .token(token)
                .expirationDate(new Date(System.currentTimeMillis() + 259_200_000))
                .build();
    }

    public Claims extractAllClaims(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new JwtIllegalTokenException("JWT token has expired");

        } catch (MalformedJwtException e) {
            throw new JwtIllegalTokenException("Malformed JWT token");

        } catch (SignatureException e) {
            throw new JwtIllegalTokenException("Invalid JWT signature");

        } catch (UnsupportedJwtException e) {
            throw new JwtIllegalTokenException("Unsupported JWT token");

        } catch (IllegalArgumentException e) {
            throw new JwtIllegalTokenException("JWT token is null or empty");
        }
    }

    public JwtTokenDto extractClaim(String token) {
        final Claims claims = extractAllClaims(token);
        Role role;
        try {
            role = Role.valueOf(
                            claims.get(TokenField.ROLE.toString(), String.class)
                    );
        }catch (IllegalArgumentException exception){
            throw new JwtIllegalTokenException("Role");
        }
        return JwtTokenDto
                .builder()
                .userId(claims.getSubject())
                .email(claims.get(TokenField.EMAIL.toString(), String.class))
                .role(role)
                .getExpiration(claims.getExpiration())
                .build();
    }

}
