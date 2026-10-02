package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100, unique = true)
    private String userName;

    @Column(name = "display_name", length = 70)
    private String displayName;

    @Column(name = "current_status", length = 255)
    private String currentStatus;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 150)
    private String organization;

    @Column(nullable = false)
    private Long follower = 0L;

    @Column(nullable = false)
    private Long following = 0L;

    @Column(nullable = false)
    private Long content = 0L;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "verified_id")
    private UUID verifiedId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @OneToMany(
            mappedBy = "profile",
            fetch = FetchType.LAZY
    )
    private List<Content> contents;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
        this.follower = 0L;
        this.following = 0L;
        this.content = 0L;
        this.verified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
