package com.smartfinance.dto;

import com.smartfinance.entity.BudgetConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetSummaryResponse {
    private List<BudgetConfig> budgets;
    private BigDecimal totalExpense;
    private List<CategoryActual> categoryActuals;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryActual {
        private String category;
        private BigDecimal amount;
    }
}
