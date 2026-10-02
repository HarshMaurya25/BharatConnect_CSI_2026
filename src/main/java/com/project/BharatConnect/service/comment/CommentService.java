package com.project.BharatConnect.service.comment;

import com.project.BharatConnect.dto.comment.CommentCreateRequest;
import com.project.BharatConnect.dto.comment.CommentResponseDto;
import com.project.BharatConnect.dto.comment.CommentUpdateRequest;
import com.project.BharatConnect.dto.common.CursorPageResponse;
import com.project.BharatConnect.entity.Comment;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.error.exception.*;
import com.project.BharatConnect.event.CommentCreatedEvent;
import com.project.BharatConnect.mapper.CommentMapper;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.counter.ContentCounterRepository;
import com.project.BharatConnect.service.ratelimit.RateLimiterService;
import com.project.BharatConnect.service.user.UserDetail;
import com.project.BharatConnect.util.CursorUtil;
import com.project.BharatConnect.util.CursorUtil.Cursor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ContentRepository contentRepository;
    private final ProfileRepository profileRepository;
    private final ContentCounterRepository contentCounterRepository;
    private final CommentMapper commentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimiterService rateLimiterService;

    @Value("${app.comments.max-replies:2}")
    private int maxReplies = 2;

    @Transactional
    public CommentResponseDto createComment(UUID contentId, CommentCreateRequest request) {
        UUID profileId = getCurrentProfileId();
        rateLimiterService.checkCommentRateLimit(profileId);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found"));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));

        String text = validateAndTrimText(request.getText());

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = validateParentComment(contentId, request.getParentCommentId());
            int updated = contentCounterRepository.tryIncrementReplyCount(parent.getId(), maxReplies);
            if (updated == 0) {
                throw new ReplyLimitReachedException(
                        "This comment already has the maximum number of replies (" + maxReplies + ")"
                );
            }
        }

        contentCounterRepository.incrementContentCommentCount(contentId);

        Comment comment = Comment.builder()
                .content(content)
                .profile(profile)
                .parentComment(parent)
                .text(text)
                .likeCount(0L)
                .replyCount(0L)
                .deleted(false)
                .edited(false)
                .build();

        commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentCreatedEvent(
                comment.getId(),
                contentId,
                profileId,
                parent != null ? parent.getId() : null
        ));

        return commentMapper.toResponse(comment, false);
    }

    @Transactional
    public CommentResponseDto editComment(UUID commentId, CommentUpdateRequest request) {
        UUID profileId = getCurrentProfileId();

        Comment comment = commentRepository.findByIdWithProfileAndParent(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        if (comment.isDeleted()) {
            throw new ConflictException("Cannot edit a deleted comment");
        }

        if (!comment.getProfile().getUserId().equals(profileId)) {
            throw new AccessDeniedException("Operation not permitted");
        }

        String text = validateAndTrimText(request.getText());
        comment.setText(text);
        comment.setEdited(true);
        comment.setUpdatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        boolean likedByMe = commentLikeRepository.existsByCommentIdAndProfileUserId(commentId, profileId);
        return commentMapper.toResponse(comment, likedByMe);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        UUID profileId = getCurrentProfileId();
        boolean isAdmin = isCurrentUserAdmin();

        Comment comment = commentRepository.findByIdWithProfileAndParent(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        if (comment.isDeleted()) {
            throw new ConflictException("Comment already deleted");
        }

        UUID authorId = comment.getProfile().getUserId();
        UUID contentOwnerId = comment.getContent().getProfile().getUserId();

        if (!isAdmin && !profileId.equals(authorId) && !profileId.equals(contentOwnerId)) {
            throw new AccessDeniedException("Operation not permitted");
        }

        contentCounterRepository.decrementContentCommentCount(comment.getContent().getId());

        if (comment.getParentComment() != null) {
            // Deleting a reply is always a hard delete
            UUID parentId = comment.getParentComment().getId();
            contentCounterRepository.decrementCommentReplyCount(parentId);
            commentLikeRepository.deleteByCommentId(commentId);
            commentRepository.delete(comment);
        } else {
            // Top-level comment
            boolean hasReplies = comment.getReplyCount() > 0 || commentRepository.existsByParentCommentId(commentId);
            if (hasReplies) {
                // Soft delete
                comment.setDeleted(true);
                comment.setText("[deleted]");
                commentRepository.save(comment);
            } else {
                // Hard delete
                commentLikeRepository.deleteByCommentId(commentId);
                commentRepository.delete(comment);
            }
        }
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponseDto> getTopLevelComments(UUID contentId, String cursorStr, int limit) {
        if (!contentRepository.existsById(contentId)) {
            throw new ContentNotFoundException("Content not found");
        }

        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 50);
        Pageable pageable = PageRequest.of(0, effectiveLimit + 1);

        Cursor cursor = CursorUtil.decode(cursorStr);
        List<Comment> comments = cursor == null
                ? commentRepository.findTopLevelCommentsInitial(contentId, pageable)
                : commentRepository.findTopLevelCommentsWithCursor(contentId, cursor.createdAt(), cursor.id(), pageable);

        boolean hasMore = comments.size() > effectiveLimit;
        List<Comment> resultComments = hasMore ? comments.subList(0, effectiveLimit) : comments;

        String nextCursor = null;
        if (hasMore && !resultComments.isEmpty()) {
            Comment last = resultComments.get(resultComments.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        UUID currentUserId = getOptionalCurrentProfileId();
        Set<UUID> likedCommentIds = getLikedCommentIds(currentUserId, resultComments);

        List<CommentResponseDto> dtos = resultComments.stream()
                .map(c -> commentMapper.toResponse(c, likedCommentIds))
                .toList();

        return new CursorPageResponse<>(dtos, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponseDto> getReplies(UUID commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException("Comment not found");
        }

        List<Comment> replies = commentRepository.findRepliesByParentCommentId(commentId);
        UUID currentUserId = getOptionalCurrentProfileId();
        Set<UUID> likedCommentIds = getLikedCommentIds(currentUserId, replies);

        List<CommentResponseDto> dtos = replies.stream()
                .map(c -> commentMapper.toResponse(c, likedCommentIds))
                .toList();

        return new CursorPageResponse<>(dtos, null, false);
    }

    private Comment validateParentComment(UUID contentId, UUID parentCommentId) {
        Comment parent = commentRepository.findByIdWithProfileAndParent(parentCommentId)
                .orElseThrow(() -> new CommentNotFoundException("Parent comment not found"));

        if (!parent.getContent().getId().equals(contentId)) {
            throw new InvalidRequestException("Parent comment does not belong to the specified content");
        }

        if (parent.getParentComment() != null) {
            throw new NestingLimitExceededException("Replies cannot be nested");
        }

        if (parent.isDeleted()) {
            throw new ConflictException("Cannot reply to a deleted comment");
        }

        return parent;
    }

    private String validateAndTrimText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new InvalidRequestException("Comment text cannot be blank");
        }
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidRequestException("Comment text cannot be blank");
        }
        if (trimmed.length() > 1000) {
            throw new InvalidRequestException("Comment text cannot exceed 1000 characters");
        }
        return trimmed;
    }

    private Set<UUID> getLikedCommentIds(UUID currentUserId, List<Comment> comments) {
        if (currentUserId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<UUID> ids = comments.stream().map(Comment::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(commentLikeRepository.findLikedCommentIdsByProfile(currentUserId, ids));
    }

    private UUID getCurrentProfileId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetail ud) || ud.getUser() == null) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        return ud.getUser().getUserId();
    }

    private UUID getOptionalCurrentProfileId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetail ud && ud.getUser() != null) {
            return ud.getUser().getUserId();
        }
        return null;
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
