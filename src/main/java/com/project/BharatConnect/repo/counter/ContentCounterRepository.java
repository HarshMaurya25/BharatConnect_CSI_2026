package com.project.BharatConnect.repo.counter;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ContentCounterRepository {

    private final EntityManager entityManager;

    public void incrementContentLikeCount(UUID contentId) {
        entityManager.createQuery(
                "UPDATE Content c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id"
        ).setParameter("id", contentId).executeUpdate();
    }

    public void decrementContentLikeCount(UUID contentId) {
        entityManager.createQuery(
                "UPDATE Content c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :id"
        ).setParameter("id", contentId).executeUpdate();
    }

    public void incrementContentCommentCount(UUID contentId) {
        entityManager.createQuery(
                "UPDATE Content c SET c.commentCount = c.commentCount + 1 WHERE c.id = :id"
        ).setParameter("id", contentId).executeUpdate();
    }

    public void decrementContentCommentCount(UUID contentId) {
        entityManager.createQuery(
                "UPDATE Content c SET c.commentCount = CASE WHEN c.commentCount > 0 THEN c.commentCount - 1 ELSE 0 END WHERE c.id = :id"
        ).setParameter("id", contentId).executeUpdate();
    }

    public void incrementCommentLikeCount(UUID commentId) {
        entityManager.createQuery(
                "UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id"
        ).setParameter("id", commentId).executeUpdate();
    }

    public void decrementCommentLikeCount(UUID commentId) {
        entityManager.createQuery(
                "UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :id"
        ).setParameter("id", commentId).executeUpdate();
    }

    public int tryIncrementReplyCount(UUID commentId, long max) {
        return entityManager.createQuery(
                "UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id AND c.replyCount < :max"
        ).setParameter("id", commentId).setParameter("max", max).executeUpdate();
    }

    public void decrementCommentReplyCount(UUID commentId) {
        entityManager.createQuery(
                "UPDATE Comment c SET c.replyCount = CASE WHEN c.replyCount > 0 THEN c.replyCount - 1 ELSE 0 END WHERE c.id = :id"
        ).setParameter("id", commentId).executeUpdate();
    }
}
