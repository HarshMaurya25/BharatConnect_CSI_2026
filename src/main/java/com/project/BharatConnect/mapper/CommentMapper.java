package com.project.BharatConnect.mapper;

import com.project.BharatConnect.dto.comment.CommentResponseDto;
import com.project.BharatConnect.entity.Comment;
import com.project.BharatConnect.entity.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class CommentMapper {

    public CommentResponseDto toResponse(Comment comment, boolean likedByMe) {
        if (comment == null) {
            return null;
        }

        Profile profile = comment.getProfile();
        boolean isDeleted = comment.isDeleted();

        return CommentResponseDto.builder()
                .id(comment.getId())
                .contentId(comment.getContent() != null ? comment.getContent().getId() : null)
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .text(isDeleted ? "[deleted]" : comment.getText())
                .authorDisplayName(isDeleted || profile == null ? null : profile.getDisplayName())
                .authorUserName(isDeleted || profile == null ? null : profile.getUserName())
                .authorProfileImage(isDeleted || profile == null ? null : profile.getProfileImage())
                .authorProfileId(isDeleted || profile == null ? null : profile.getUserId())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .likedByMe(likedByMe)
                .deleted(isDeleted)
                .edited(comment.isEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public CommentResponseDto toResponse(Comment comment, Set<UUID> likedCommentIds) {
        boolean likedByMe = likedCommentIds != null && comment.getId() != null && likedCommentIds.contains(comment.getId());
        return toResponse(comment, likedByMe);
    }
}
