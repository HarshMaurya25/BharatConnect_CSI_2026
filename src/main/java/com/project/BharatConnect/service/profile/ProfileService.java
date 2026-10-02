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
import com.project.BharatConnect.service.media.MediaService;
import com.project.BharatConnect.service.user.UserDetail;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final MediaService mediaService;

    @Transactional
    public Boolean createProfileWithUsername(String username){
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();
        log.info("User id ; {}",userId.toString());
        User user = userRepository.getReferenceById(userId);

        if (profileRepository.existsById(userId)) {
            throw new UserAlreadyExistException(username);
        }

        try {
            Profile profile = Profile.builder()
                    .user(user)
                    .userName(username)
                    .build();

            profileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameNotUniqueException(username);
        }
        return true;
    }

    @Transactional
    public ProfileResponseDto updateProfile(ProfileCreateRequestDto requestDto){
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();

        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Profile not found")
                );

        profile.setDisplayName(requestDto.getDisplayName());
        profile.setBio(requestDto.getBio());
        profile.setCurrentStatus(requestDto.getCurrentStatus());
        profile.setOrganization(requestDto.getOrganization());

        profileRepository.save(profile);

        return profileMapper.toResponse(profile);
    }

    @Transactional
    public String uploadProfile(MultipartFile profileImage) {

        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();

        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Profile not found")
                );

        Map<String, Object> result =
                mediaService.uploadProfileImage(
                        profileImage,
                        userId
                );

        String url = (String) result.get("secure_url");

        profile.setProfileImage(url);

        profileRepository.save(profile);

        return url;
    }

}
