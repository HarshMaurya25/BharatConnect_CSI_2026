package com.project.BharatConnect.repo;

import com.project.BharatConnect.dto.social.LikerDto;
import com.project.BharatConnect.entity.ContentLike;
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
public interface ContentLikeRepository extends JpaRepository<ContentLike, UUID> {

    boolean existsByContentIdAndProfileUserId(UUID contentId, UUID profileId);

    @Modifying
    @Query("DELETE FROM ContentLike l WHERE l.content.id = :contentId AND l.profile.userId = :profileId")
    int deleteByContentIdAndProfileUserId(@Param("contentId") UUID contentId, @Param("profileId") UUID profileId);

    @Query("SELECT l.content.id FROM ContentLike l WHERE l.profile.userId = :profileId AND l.content.id IN :contentIds")
    List<UUID> findLikedContentIdsByProfile(@Param("profileId") UUID profileId, @Param("contentIds") Collection<UUID> contentIds);

    @Query(
            value = "SELECT new com.project.BharatConnect.dto.social.LikerDto(p.userId, p.userName, p.displayName) " +
                    "FROM ContentLike l JOIN l.profile p " +
                    "WHERE l.content.id = :contentId " +
                    "ORDER BY l.createdAt DESC, l.id DESC",
            countQuery = "SELECT COUNT(l) FROM ContentLike l WHERE l.content.id = :contentId"
    )
    Page<LikerDto> findLikersByContentId(@Param("contentId") UUID contentId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ContentLike l WHERE l.content.id = :contentId")
    void deleteByContentId(@Param("contentId") UUID contentId);
}
