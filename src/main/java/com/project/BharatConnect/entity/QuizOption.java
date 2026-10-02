package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "quiz_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "option_text", length = 100, nullable = false)
    private String optionText;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "correct", nullable = false)
    private Boolean correct;

    @Builder.Default
    @Column(name = "pick_count", nullable = false)
    private Long pickCount = 0L;
}
