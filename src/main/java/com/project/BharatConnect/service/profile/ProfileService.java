package com.project.BharatConnect.service.profile;

import com.project.BharatConnect.dto.profile.ProfileCreateRequestDto;
import com.project.BharatConnect.dto.profile.ProfileResponseDto;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.UserAlreadyExistException;
import com.project.BharatConnect.error.exception.UsernameNotUniqueException;
import com.project.BharatConnect.mapper.ProfileMapper;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.UserRepository;
import com.project.BharatConnect.service.user.UserDetail;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    @Transactional
    public Boolean createProfileWithUsername(String username){
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();

        User user = userRepository.getReferenceById(userId);

        if (profileRepository.existsById(userId)) {
            throw new UserAlreadyExistException(username);
        }

        try {
            Profile profile = Profile
                    .builder()
                    .userId(userId)
                    .user(user)
                    .userName(username)
                    .build();

            profileRepository.save(profile);
        }catch (DataIntegrityViolationException e){
            throw new UsernameNotUniqueException(username);
        }
        return true;
    }

    @Transactional
    public ProfileResponseDto createFullProfile(ProfileCreateRequestDto requestDto){
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();

        Profile profile = profileRepository.getReferenceById(userId);

        profile.setDisplayName(requestDto.getDisplayName());
        profile.setBio(requestDto.getBio());
        profile.setCurrentStatus(requestDto.getCurrentStatus());
        profile.setOrganization(requestDto.getOrganization());

        profileRepository.save(profile);

        return profileMapper.toResponse(profile);
    }

}
