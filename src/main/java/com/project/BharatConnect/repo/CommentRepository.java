package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.profile LEFT JOIN FETCH c.parentComment WHERE c.id = :id")
    Optional<Comment> findByIdWithProfileAndParent(@Param("id") UUID id);

    @Query("SELECT c FROM Comment c JOIN FETCH c.profile WHERE c.content.id = :contentId AND c.parentComment IS NULL ORDER BY c.createdAt DESC, c.id DESC")
    List<Comment> findTopLevelCommentsInitial(@Param("contentId") UUID contentId, Pageable pageable);

    @Query("""
        SELECT c FROM Comment c JOIN FETCH c.profile
        WHERE c.content.id = :contentId AND c.parentComment IS NULL
        AND (c.createdAt < :cursorCreatedAt OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId))
        ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<Comment> findTopLevelCommentsWithCursor(
            @Param("contentId") UUID contentId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT c FROM Comment c JOIN FETCH c.profile WHERE c.parentComment.id = :parentCommentId ORDER BY c.createdAt ASC, c.id ASC")
    List<Comment> findRepliesByParentCommentId(@Param("parentCommentId") UUID parentCommentId);

    boolean existsByParentCommentId(UUID parentCommentId);

    long countByParentCommentId(UUID parentCommentId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.content.id = :contentId")
    void deleteByContentId(@Param("contentId") UUID contentId);
}
