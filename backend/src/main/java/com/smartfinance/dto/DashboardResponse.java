package com.smartfinance.dto;

import com.smartfinance.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private List<CategoryStat> categoryStats;
    private List<MonthlyTrend> monthlyTrend;
    private List<Transaction> recentTransactions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStat {
        private String category;
        private BigDecimal amount;
        private double percentage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyTrend {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
    }
}
