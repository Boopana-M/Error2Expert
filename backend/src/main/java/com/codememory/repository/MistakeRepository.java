package com.codememory.repository;

import com.codememory.entity.Mistake;
import com.codememory.entity.enums.MistakeCategory;
import com.codememory.entity.enums.MistakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MistakeRepository extends JpaRepository<Mistake, Long> {
    List<Mistake> findBySubmissionId(Long submissionId);

    @Query("SELECT m FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId ORDER BY m.createdAt DESC")
    List<Mistake> findByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId AND m.category = :category")
    List<Mistake> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") MistakeCategory category);

    @Query("SELECT COUNT(m) FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId AND s.problem.id = :problemId AND m.status != 'RESOLVED'")
    List<Mistake> findUnresolvedByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Query("SELECT m FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId AND m.status = :status")
    List<Mistake> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") MistakeStatus status);

    @Query("SELECT COUNT(m) FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId AND m.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") MistakeStatus status);

    @Query("SELECT COUNT(m) > 0 FROM Mistake m JOIN m.submission s WHERE s.user.id = :userId AND m.category = :category AND m.status = 'RESOLVED'")
    boolean hasPreviouslyResolvedCategory(@Param("userId") Long userId, @Param("category") MistakeCategory category);

    @Query("SELECT m FROM Mistake m JOIN m.submission s JOIN s.problem p JOIN p.topics t WHERE s.user.id = :userId AND t = :topic AND m.status != 'RESOLVED'")
    List<Mistake> findUnresolvedByUserIdAndTopic(@Param("userId") Long userId, @Param("topic") String topic);
}
