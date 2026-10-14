package com.codememory.repository;

import com.codememory.entity.Mistake;
import com.codememory.entity.enums.MistakeCategory;
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
}
