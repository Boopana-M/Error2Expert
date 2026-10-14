package com.codememory.repository;

import com.codememory.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findByTitleIgnoreCase(String title);
    Optional<Problem> findByLeetcodeId(String leetcodeId);
}
