package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE QuizOption o SET o.pickCount = o.pickCount + 1 WHERE o.id = :id")
    void incrementPickCount(@Param("id") UUID id);
}
