package com.smartfinance.repository;

import com.smartfinance.entity.BudgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetConfigRepository extends JpaRepository<BudgetConfig, Long> {
    List<BudgetConfig> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    Optional<BudgetConfig> findByUserIdAndCategoryIsNullAndYearAndMonth(Long userId, Integer year, Integer month);
    Optional<BudgetConfig> findByUserIdAndCategoryAndYearAndMonth(Long userId, String category, Integer year, Integer month);
}
