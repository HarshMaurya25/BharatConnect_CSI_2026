package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "point_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_point_transactions_profile_action_source",
                        columnNames = {"profile_id", "action", "source_id"}
                )
        },
        indexes = {
                @Index(name = "idx_pt_profile_created", columnList = "profile_id, created_at DESC, id"),
                @Index(name = "idx_pt_created_at", columnList = "created_at"),
                @Index(name = "idx_pt_source_id", columnList = "source_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private PointAction action;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private PointSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
