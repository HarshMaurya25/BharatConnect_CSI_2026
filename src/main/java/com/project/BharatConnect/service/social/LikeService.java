package com.project.BharatConnect.service.social;

import com.project.BharatConnect.dto.social.LikeResponseDto;
import com.project.BharatConnect.dto.social.LikerDto;
import com.project.BharatConnect.entity.Comment;
import com.project.BharatConnect.entity.CommentLike;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentLike;
import com.project.BharatConnect.error.exception.CommentNotFoundException;
import com.project.BharatConnect.error.exception.ConflictException;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.event.CommentLikedEvent;
import com.project.BharatConnect.event.CommentUnlikedEvent;
import com.project.BharatConnect.event.ContentLikedEvent;
import com.project.BharatConnect.event.ContentUnlikedEvent;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.counter.ContentCounterRepository;
import com.project.BharatConnect.service.ratelimit.RateLimiterService;
import com.project.BharatConnect.service.user.UserDetail;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class LikeService {

    private final ContentRepository contentRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ProfileRepository profileRepository;
    private final ContentCounterRepository contentCounterRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimiterService rateLimiterService;

    @Transactional
    public LikeResponseDto likeContent(UUID contentId) {
        UUID profileId = getCurrentProfileId();
        rateLimiterService.checkLikeRateLimit(profileId);

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));

        if (!contentLikeRepository.existsByContentIdAndProfileUserId(contentId, profileId)) {
            try {
                ContentLike like = ContentLike.builder()
                        .content(content)
                        .profile(profileRepository.getReferenceById(profileId))
                        .build();
                contentLikeRepository.saveAndFlush(like);
                contentCounterRepository.incrementContentLikeCount(contentId);
                UUID ownerId = content.getProfile() != null ? content.getProfile().getUserId() : null;
                eventPublisher.publishEvent(new ContentLikedEvent(contentId, profileId, ownerId));
            } catch (DataIntegrityViolationException e) {
                log.debug("Concurrent duplicate like for content {} by user {}", contentId, profileId);
            }
        }

        long currentLikeCount = contentRepository.findById(contentId)
                .map(Content::getLikeCount)
                .orElse(0L);

        return new LikeResponseDto(true, currentLikeCount);
    }

    @Transactional
    public LikeResponseDto unlikeContent(UUID contentId) {
        UUID profileId = getCurrentProfileId();

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));

        int deletedCount = contentLikeRepository.deleteByContentIdAndProfileUserId(contentId, profileId);
        if (deletedCount > 0) {
            contentCounterRepository.decrementContentLikeCount(contentId);
            UUID ownerId = content.getProfile() != null ? content.getProfile().getUserId() : null;
            eventPublisher.publishEvent(new ContentUnlikedEvent(contentId, profileId, ownerId));
        }

        long currentLikeCount = contentRepository.findById(contentId)
                .map(Content::getLikeCount)
                .orElse(0L);

        return new LikeResponseDto(false, currentLikeCount);
    }

    @Transactional(readOnly = true)
    public Page<LikerDto> getContentLikers(UUID contentId, int page, int size) {
        if (!contentRepository.existsById(contentId)) {
            throw new ContentNotFoundException("Content not found");
        }

        int effectiveSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), effectiveSize);

        return contentLikeRepository.findLikersByContentId(contentId, pageable);
    }

    @Transactional
    public LikeResponseDto likeComment(UUID commentId) {
        UUID profileId = getCurrentProfileId();
        rateLimiterService.checkLikeRateLimit(profileId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        if (comment.isDeleted()) {
            throw new ConflictException("Cannot like a deleted comment");
        }

        if (!commentLikeRepository.existsByCommentIdAndProfileUserId(commentId, profileId)) {
            try {
                CommentLike like = CommentLike.builder()
                        .comment(comment)
                        .profile(profileRepository.getReferenceById(profileId))
                        .build();
                commentLikeRepository.saveAndFlush(like);
                contentCounterRepository.incrementCommentLikeCount(commentId);
                UUID authorId = comment.getProfile() != null ? comment.getProfile().getUserId() : null;
                eventPublisher.publishEvent(new CommentLikedEvent(commentId, profileId, authorId));
            } catch (DataIntegrityViolationException e) {
                log.debug("Concurrent duplicate like for comment {} by user {}", commentId, profileId);
            }
        }

        long currentLikeCount = commentRepository.findById(commentId)
                .map(Comment::getLikeCount)
                .orElse(0L);

        return new LikeResponseDto(true, currentLikeCount);
    }

    @Transactional
    public LikeResponseDto unlikeComment(UUID commentId) {
        UUID profileId = getCurrentProfileId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        int deletedCount = commentLikeRepository.deleteByCommentIdAndProfileUserId(commentId, profileId);
        if (deletedCount > 0) {
            contentCounterRepository.decrementCommentLikeCount(commentId);
            UUID authorId = comment.getProfile() != null ? comment.getProfile().getUserId() : null;
            eventPublisher.publishEvent(new CommentUnlikedEvent(commentId, profileId, authorId));
        }

        long currentLikeCount = commentRepository.findById(commentId)
                .map(Comment::getLikeCount)
                .orElse(0L);

        return new LikeResponseDto(false, currentLikeCount);
    }

    @Transactional(readOnly = true)
    public Page<LikerDto> getCommentLikers(UUID commentId, int page, int size) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException("Comment not found");
        }

        int effectiveSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), effectiveSize);

        return commentLikeRepository.findLikersByCommentId(commentId, pageable);
    }

    private UUID getCurrentProfileId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetail ud) || ud.getUser() == null) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        return ud.getUser().getUserId();
    }
}
