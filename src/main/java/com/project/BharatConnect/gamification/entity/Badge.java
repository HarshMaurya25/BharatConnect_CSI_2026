package com.project.BharatConnect.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "badges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_badges_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "icon", nullable = false, length = 200)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private BadgeTier tier;

    @Column(name = "category", length = 50)
    private String category;

    @Builder.Default
    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean isHidden() {
        return hidden;
    }

    public boolean getHidden() {
        return hidden;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }
}
