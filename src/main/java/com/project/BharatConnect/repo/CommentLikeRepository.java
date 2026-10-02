package com.project.BharatConnect.repo;

import com.project.BharatConnect.dto.social.LikerDto;
import com.project.BharatConnect.entity.CommentLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByCommentIdAndProfileUserId(UUID commentId, UUID profileId);

    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.comment.id = :commentId AND l.profile.userId = :profileId")
    int deleteByCommentIdAndProfileUserId(@Param("commentId") UUID commentId, @Param("profileId") UUID profileId);

    @Query("SELECT l.comment.id FROM CommentLike l WHERE l.profile.userId = :profileId AND l.comment.id IN :commentIds")
    List<UUID> findLikedCommentIdsByProfile(@Param("profileId") UUID profileId, @Param("commentIds") Collection<UUID> commentIds);

    @Query(
            value = "SELECT new com.project.BharatConnect.dto.social.LikerDto(p.userId, p.userName, p.displayName) " +
                    "FROM CommentLike l JOIN l.profile p " +
                    "WHERE l.comment.id = :commentId " +
                    "ORDER BY l.createdAt DESC, l.id DESC",
            countQuery = "SELECT COUNT(l) FROM CommentLike l WHERE l.comment.id = :commentId"
    )
    Page<LikerDto> findLikersByCommentId(@Param("commentId") UUID commentId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") UUID commentId);

    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.comment.content.id = :contentId")
    void deleteByContentId(@Param("contentId") UUID contentId);
}
