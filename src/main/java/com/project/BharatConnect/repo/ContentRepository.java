package com.project.BharatConnect.repo;

import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content , UUID> {
    @Query(""" 
            SELECT new com.project.BharatConnect.dto.content.ContentResponseDto(
                c.id,p.userName,p.displayName,c.contentName,c.text,c.contentType,c.contentUrl,c.likeCount,c.commentCount,c.updatedAt
            )
            FROM Content c
            JOIN c.profile p
            WHERE p.userId = :userId
            ORDER BY c.updatedAt DESC
            """)
    Page<ContentResponseDto> findByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
        SELECT new com.project.BharatConnect.dto.content.ContentResponseDto(
            c.id,p.userName,p.displayName,c.contentName,c.text,c.contentType,c.contentUrl,c.likeCount,c.commentCount,c.updatedAt
        )
        FROM Content c
        JOIN c.profile p
        WHERE c.id = :contentId
        """)
    Optional<ContentResponseDto> findContentById(
            @Param("contentId") UUID contentId
    );
}
