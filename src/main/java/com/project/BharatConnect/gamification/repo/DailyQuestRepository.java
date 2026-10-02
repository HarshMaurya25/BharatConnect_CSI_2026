package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.DailyQuest;
import com.project.BharatConnect.gamification.entity.QuestMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyQuestRepository extends JpaRepository<DailyQuest, UUID> {

    Optional<DailyQuest> findByCode(String code);

    List<DailyQuest> findByActiveTrue();

    List<DailyQuest> findByActiveTrueAndMetric(QuestMetric metric);
}
