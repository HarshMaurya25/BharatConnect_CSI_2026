package com.project.BharatConnect.entity;

import com.project.BharatConnect.util.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(
            mappedBy = "user",
            fetch = FetchType.LAZY
    )
    private Profile profile;

    @Column(name = "date_of_creation", nullable = false)
    private LocalDateTime dateOfCreation;

    @Column(name = "date_of_last_update", nullable = false)
    private LocalDateTime dateOfLastUpdate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.dateOfCreation = now;
        this.dateOfLastUpdate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateOfLastUpdate = LocalDateTime.now();
    }
}