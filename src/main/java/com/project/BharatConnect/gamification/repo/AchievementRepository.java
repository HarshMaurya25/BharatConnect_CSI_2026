package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.Achievement;
import com.project.BharatConnect.gamification.entity.AchievementMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    Optional<Achievement> findByCode(String code);

    @Query("SELECT a FROM Achievement a LEFT JOIN FETCH a.badge WHERE a.active = true")
    List<Achievement> findAllActiveWithBadge();

    @Query("SELECT a FROM Achievement a LEFT JOIN FETCH a.badge WHERE a.active = true AND a.metric IN (:metrics)")
    List<Achievement> findActiveByMetricsWithBadge(@Param("metrics") Collection<AchievementMetric> metrics);
}
