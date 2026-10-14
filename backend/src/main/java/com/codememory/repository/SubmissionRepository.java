package com.codememory.repository;

import com.codememory.entity.Submission;
import com.codememory.entity.enums.SubmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtAsc(Long userId, Long problemId);
    List<Submission> findByUserIdAndResult(Long userId, SubmissionResult result);
    long countByUserId(Long userId);
    long countByUserIdAndResult(Long userId, SubmissionResult result);
}
