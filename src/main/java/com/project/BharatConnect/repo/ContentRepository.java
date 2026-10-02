package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    @Query(
            value = "SELECT c FROM Content c JOIN FETCH c.profile WHERE c.profile.userId = :userId ORDER BY c.createdAt DESC",
            countQuery = "SELECT count(c) FROM Content c WHERE c.profile.userId = :userId"
    )
    Page<Content> findByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("SELECT c FROM Content c JOIN FETCH c.profile WHERE c.id = :contentId")
    Optional<Content> findContentById(
            @Param("contentId") UUID contentId
    );

    @Query("SELECT COUNT(c) > 0 FROM Content c WHERE c.profile.userId = :profileId AND c.contentType = :contentType AND c.parentContent.id = :parentId AND (c.text IS NULL OR TRIM(c.text) = '')")
    boolean existsPlainRepost(
            @Param("profileId") UUID profileId,
            @Param("contentType") ContentType contentType,
            @Param("parentId") UUID parentId
    );

    @Modifying
    @Query("UPDATE Content c SET c.repostCount = c.repostCount + 1 WHERE c.id = :id")
    void incrementRepostCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Content c SET c.repostCount = CASE WHEN c.repostCount > 0 THEN c.repostCount - 1 ELSE 0 END WHERE c.id = :id")
    void decrementRepostCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Content c SET c.parentContent = null WHERE c.parentContent.id = :id")
    void clearParentContentReferences(@Param("id") UUID id);
}
