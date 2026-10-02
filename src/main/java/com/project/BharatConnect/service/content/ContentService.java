package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidMediaException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.mapper.ContentMapper;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.service.media.MediaService;
import com.project.BharatConnect.service.user.UserDetail;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@AllArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ProfileRepository profileRepository;
    private final MediaService mediaService;
    private final PollService pollService;
    private final ContentMapper contentMapper;

    @Transactional
    public ContentResponseDto uploadContent(ContentCreateRequest req, MultipartFile file) {
        UserDetail ud = (UserDetail) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        Profile profile = profileRepository.findById(ud.getUser().getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found"));

        ContentType type = req.getContentType();
        if (type == null) {
            throw new InvalidRequestException("contentType is required");
        }

        Content resolvedParent = null;
        if (type == ContentType.REPOST) {
            if (req.getParentContentId() == null) {
                throw new InvalidRequestException("parentContentId is required for repost");
            }
            Content parent = contentRepository.findById(req.getParentContentId())
                    .orElseThrow(() -> new ContentNotFoundException("Parent content not found"));

            if (parent.getContentType() == ContentType.REPOST) {
                Content original = parent.getParentContent();
                if (original == null) {
                    throw new InvalidRequestException("Original content no longer exists");
                }
                resolvedParent = original;
            } else {
                resolvedParent = parent;
            }

            boolean hasText = req.getText() != null && !req.getText().isBlank();
            if (!hasText) {
                if (contentRepository.existsPlainRepost(profile.getUserId(), ContentType.REPOST, resolvedParent.getId())) {
                    throw new InvalidRequestException("You have already reposted this content");
                }
            }
        }

        validate(type, req, file);

        Content content = Content.builder()
                .profile(profile)
                .contentName(req.getContentName())
                .text(req.getText() != null && !req.getText().isBlank() ? req.getText() : null)
                .contentType(type)
                .parentContent(resolvedParent)
                .build();

        contentRepository.save(content);

        switch (type) {
            case IMAGE -> {
                Map<String, Object> result = mediaService.uploadPostImage(file, content.getId());
                content.setContentUrl((String) result.get("secure_url"));
            }
            case VIDEO -> {
                Map result = mediaService.uploadVideo(file, content.getId());
                content.setContentUrl((String) result.get("secure_url"));
            }
            case POLL -> {
                Poll poll = pollService.createPoll(content, req);
                content.setPoll(poll);
            }
            case REPOST -> {
                contentRepository.incrementRepostCount(resolvedParent.getId());
            }
            case TEXT -> {
            }
        }

        return contentMapper.toResponse(content, profile.getDisplayName(), profile.getUserName(), profile.getUserId(), false);
    }

    private void validate(ContentType type, ContentCreateRequest req, MultipartFile file) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = req.getText() != null && !req.getText().isBlank();

        switch (type) {
            case TEXT -> {
                if (!hasText) throw new InvalidRequestException("Text cannot be empty");
                if (hasFile) throw new InvalidRequestException("Text posts cannot have a file");
            }
            case IMAGE -> requireMime(file, "image/", "Image file required");
            case VIDEO -> requireMime(file, "video/", "Video file required");
            case POLL -> {
                if (!hasText) throw new InvalidRequestException("Poll question is required");
                if (hasFile) throw new InvalidRequestException("Polls cannot have a file");
                List<String> opts = req.getPollOptions();
                if (opts == null || opts.size() < 2 || opts.size() > 6)
                    throw new InvalidRequestException("Poll needs 2 to 6 options");
                long distinct = opts.stream().filter(o -> o != null && !o.isBlank())
                        .map(String::trim).map(String::toLowerCase).distinct().count();
                if (distinct != opts.size())
                    throw new InvalidRequestException("Poll options must be non-blank and unique");
                if (req.getPollDurationHours() != null && req.getPollDurationHours() <= 0)
                    throw new InvalidRequestException("Poll duration must be positive");
            }
            case REPOST -> {
                if (hasFile) throw new InvalidRequestException("Reposts cannot have a file");
                if (hasText && req.getText().length() > 500)
                    throw new InvalidRequestException("Quote text cannot exceed 500 characters");
            }
        }
    }

    private void requireMime(MultipartFile file, String prefix, String msg) {
        if (file == null || file.isEmpty()) throw new InvalidMediaException(msg);
        String mime = file.getContentType();
        if (mime == null || !mime.startsWith(prefix)) throw new InvalidMediaException(msg);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponseDto> getContentByUserId(UUID userId, int page, int size) {
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> contentPage = contentRepository.findByUserId(userId, pageable);

        Set<UUID> likedContentIds = Collections.emptySet();
        if (currentUserId != null && !contentPage.isEmpty()) {
            List<UUID> contentIds = contentPage.getContent().stream().map(Content::getId).toList();
            likedContentIds = new HashSet<>(contentLikeRepository.findLikedContentIdsByProfile(currentUserId, contentIds));
        }

        final Set<UUID> finalLikedIds = likedContentIds;
        return contentPage.map(c -> {
            boolean likedByMe = finalLikedIds.contains(c.getId());
            return contentMapper.toResponse(c, currentUserId, likedByMe);
        });
    }

    @Transactional(readOnly = true)
    public ContentResponseDto getContentById(UUID contentId) {
        UUID currentUserId = getCurrentUserId();
        Content content = contentRepository.findContentById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));
        boolean likedByMe = currentUserId != null &&
                contentLikeRepository.existsByContentIdAndProfileUserId(contentId, currentUserId);
        return contentMapper.toResponse(content, currentUserId, likedByMe);
    }

    @Transactional
    public Boolean deleteContent(UUID contentId) {
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        UUID userId = userDetail.getUser().getUserId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));

        UUID ownerId = content.getProfile().getUserId();

        if (!isAdmin && !userId.equals(ownerId)) {
            throw new AccessDeniedException("Operation not permitted");
        }

        if (content.getContentType() == ContentType.REPOST && content.getParentContent() != null) {
            contentRepository.decrementRepostCount(content.getParentContent().getId());
        }

        contentRepository.clearParentContentReferences(contentId);

        // Explicit cleanup for likes, comment likes, and comments
        commentLikeRepository.deleteByContentId(contentId);
        commentRepository.deleteByContentId(contentId);
        contentLikeRepository.deleteByContentId(contentId);

        contentRepository.delete(content);
        return true;
    }

    @Transactional
    public void votePoll(UUID pollId, UUID optionId) {
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        UUID profileId = userDetail.getUser().getUserId();
        pollService.vote(pollId, optionId, profileId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetail ud && ud.getUser() != null) {
            return ud.getUser().getUserId();
        }
        return null;
    }
}
