package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile , UUID> {
    Optional<Profile> findByUserId(UUID userId);

    Optional<Profile> findByUserName(String username);

    boolean existsByUserId(UUID userId);
}
