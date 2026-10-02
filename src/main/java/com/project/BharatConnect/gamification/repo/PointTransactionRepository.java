package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.PointAction;
import com.project.BharatConnect.gamification.entity.PointSourceType;
import com.project.BharatConnect.gamification.entity.PointTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    boolean existsByProfileIdAndActionAndSourceId(UUID profileId, PointAction action, UUID sourceId);

    List<PointTransaction> findByProfileIdAndSourceId(UUID profileId, UUID sourceId);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.profileId = :profileId AND pt.points > 0 AND pt.createdAt >= :start AND pt.createdAt < :end AND pt.sourceType NOT IN (:excludedTypes)")
    int sumCappedPointsToday(@Param("profileId") UUID profileId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("excludedTypes") Collection<PointSourceType> excludedTypes);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.profileId = :profileId AND pt.action = :action AND pt.points > 0 AND pt.createdAt >= :start AND pt.createdAt < :end")
    int sumActionPointsToday(@Param("profileId") UUID profileId, @Param("action") PointAction action, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.profileId = :profileId ORDER BY pt.createdAt DESC, pt.id DESC")
    List<PointTransaction> findInitialHistory(@Param("profileId") UUID profileId, Pageable pageable);

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.profileId = :profileId AND (pt.createdAt < :cursorCreatedAt OR (pt.createdAt = :cursorCreatedAt AND pt.id < :cursorId)) ORDER BY pt.createdAt DESC, pt.id DESC")
    List<PointTransaction> findHistoryWithCursor(@Param("profileId") UUID profileId, @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);
}
