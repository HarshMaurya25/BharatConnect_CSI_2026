package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidMediaException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.mapper.ContentMapper;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.service.media.MediaService;
import com.project.BharatConnect.service.user.UserDetail;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final ProfileRepository profileRepository;
    private final MediaService mediaService;
    private final ContentMapper contentMapper;

    @Transactional
    public ContentResponseDto uploadContent(
            ContentCreateRequest request, MultipartFile file
    ) {
        UserDetail userDetail = (UserDetail) Objects.requireNonNull( SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        UUID userId = userDetail.getUser().getUserId();
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Profile not found")
                );

        String contentType;
        String contentUrl = null;

        Content content = Content.builder()
                .profile(profile)
                .contentName(request.getContentName())
                .text(request.getText())
                .build();

        contentRepository.save(content);

        UUID contentId = content.getId();

        if (file == null || file.isEmpty()) {
            if (request.getText() == null ||
                    request.getText().isBlank()) {
                throw new InvalidRequestException("Content cannot be empty");
            }
            contentType = "text";
        } else {
            String mimeType = file.getContentType();
            if (mimeType == null) {
                throw new InvalidMediaException("Unable to determine file type");

            } if (mimeType.startsWith("image/")) {
                Map<String, Object> result = mediaService.uploadPostImage(file, contentId);
                contentUrl = (String) result.get("secure_url");
                contentType = "image";

            } else if (mimeType.startsWith("video/")) {
                Map result = mediaService.uploadVideo(file, contentId);
                contentUrl = (String) result.get("secure_url");
                contentType = "video";
            } else {
                throw new InvalidMediaException("Only image and video files are supported");
            }
        }
        content.setContentType(contentType);
        content.setContentUrl(contentUrl);
        return contentMapper.toResponse(content , profile.getDisplayName(), profile.getUserName());
    }

    @Transactional
    public Page<ContentResponseDto> getContentByUserId( UUID userId, int page, int size ) {
        Pageable pageable = PageRequest.of(page, size);

        return contentRepository.findByUserId(
                userId,
                pageable
        );
    }

    public ContentResponseDto getContentById(UUID contentId) {
        return contentRepository.findContentById(contentId)
                .orElseThrow(() ->
                        new ContentNotFoundException( contentId.toString() )
                );
    }

    @Transactional
    public Boolean deleteContent(UUID contentId) {

        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        UUID userId = userDetail.getUser().getUserId();
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() ->
                        new ContentNotFoundException("Content not found")
                );

        UUID ownerId = content.getProfile().getUserId();

        if (!isAdmin && !userId.equals(ownerId)) {
            throw new AccessDeniedException(
                    "Operation"
            );
        }

        contentRepository.delete(content);
        return true;
    }

}
