package com.project.BharatConnect.repo;

import com.project.BharatConnect.dto.social.FollowUserResponseDto;
import com.project.BharatConnect.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    Optional<Profile> findByUserName(String username);

    boolean existsByUserId(UUID userId);

    @Query("""
            SELECT new com.project.BharatConnect.dto.social.FollowUserResponseDto(
                p.userId,
                p.userName,
                p.displayName
            )
            FROM Profile p
            WHERE LOWER(p.userName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<FollowUserResponseDto> searchProfiles(
            @Param("query") String query,
            Pageable pageable
    );
}