package com.project.BharatConnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "poll_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(name = "option_text", nullable = false, length = 100)
    private String optionText;

    @Column(name = "position", nullable = false)
    private int position;

    @Builder.Default
    @Column(name = "vote_count", nullable = false)
    private long voteCount = 0L;
}
