package com.project.BharatConnect.gamification.repo;

import com.project.BharatConnect.gamification.entity.UserQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {

    Optional<UserQuestProgress> findByProfileIdAndQuestIdAndQuestDate(UUID profileId, UUID questId, LocalDate questDate);

    List<UserQuestProgress> findByProfileIdAndQuestDate(UUID profileId, LocalDate questDate);

    @Query("SELECT uqp FROM UserQuestProgress uqp JOIN FETCH uqp.quest WHERE uqp.profileId = :profileId AND uqp.questDate = :questDate")
    List<UserQuestProgress> findByProfileIdAndQuestDateWithQuest(@Param("profileId") UUID profileId, @Param("questDate") LocalDate questDate);

    @Query("SELECT uqp FROM UserQuestProgress uqp JOIN FETCH uqp.quest WHERE uqp.profileId = :profileId AND uqp.questDate = :questDate AND uqp.quest.id IN (:questIds)")
    List<UserQuestProgress> findByProfileIdAndQuestDateAndQuestIds(@Param("profileId") UUID profileId, @Param("questDate") LocalDate questDate, @Param("questIds") Collection<UUID> questIds);
}
