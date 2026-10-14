package com.codememory.repository;

import com.codememory.entity.Recommendation;
import com.codememory.entity.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Recommendation> findByUserIdAndStatus(Long userId, RecommendationStatus status);
}
