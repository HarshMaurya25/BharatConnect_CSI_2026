package com.project.BharatConnect.service.user;

import com.project.BharatConnect.dto.security.JwtTokenDto;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.JwtIllegalTokenException;
import com.project.BharatConnect.repo.UserRepository;
import com.project.BharatConnect.util.Role;
import com.project.BharatConnect.util.TokenField;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
@AllArgsConstructor
public class AuthenticationUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByEmail(username);
            if(user == null){
                throw new UsernameNotFoundException(username);
            }
            return new UserDetail(user);
        } catch (Exception e) {
            log.error("User Detail Service : {} " ,e.getMessage());
            throw new UsernameNotFoundException(username);
        }
    }

    public UserDetails loadUserWithToken(UUID id , JwtTokenDto token) throws UsernameNotFoundException {
        User user = User.builder()
                .userId(id)
                .email(token.getEmail())
                .role(token.getRole())
                .password("AuthenticatedUser")
                .build();

        return new UserDetail(user);
    }

}
