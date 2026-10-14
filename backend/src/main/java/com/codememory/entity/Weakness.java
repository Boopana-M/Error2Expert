package com.codememory.entity;

import com.codememory.entity.enums.MistakeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weaknesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
public class Weakness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String concept;

    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_category", nullable = false, length = 60)
    private MistakeCategory mistakeCategory;

    @Column(nullable = false)
    @Builder.Default
    private Integer frequency = 1;

    @Column
    private Double confidence;

    @Column(name = "mastery_score")
    @Builder.Default
    private Double masteryScore = 100.0;

    @Column(name = "first_observed", nullable = false)
    private LocalDateTime firstObserved;

    @Column(name = "last_observed", nullable = false)
    private LocalDateTime lastObserved;
}
