package com.codememory.repository;

import com.codememory.entity.Weakness;
import com.codememory.entity.enums.MistakeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeaknessRepository extends JpaRepository<Weakness, Long> {
    List<Weakness> findByUserIdOrderByFrequencyDesc(Long userId);
    Optional<Weakness> findByUserIdAndConceptAndMistakeCategory(Long userId, String concept, MistakeCategory mistakeCategory);
    List<Weakness> findByUserIdAndMistakeCategory(Long userId, MistakeCategory mistakeCategory);
}
