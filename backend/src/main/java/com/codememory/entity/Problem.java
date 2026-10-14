package com.codememory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leetcode_id")
    private String leetcodeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String difficulty;

    private String url;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_topics", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "topic")
    @Builder.Default
    private List<String> topics = new ArrayList<>();
}
