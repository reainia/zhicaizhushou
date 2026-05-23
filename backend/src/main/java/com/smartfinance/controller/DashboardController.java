package com.smartfinance.controller;

import com.smartfinance.dto.DashboardResponse;
import com.smartfinance.entity.User;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getUserFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供令牌");
        }
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);
            LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
            LocalDate sixMonthsAgo = now.minusMonths(6).withDayOfMonth(1);

            // 当月收支汇总
            BigDecimal totalIncome = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                    user.getId(), "INCOME", monthStart, monthEnd);
            BigDecimal totalExpense = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                    user.getId(), "EXPENSE", monthStart, monthEnd);
            BigDecimal balance = totalIncome.subtract(totalExpense);

            // 当月分类统计
            List<Object[]> categoryRaw = transactionRepository.sumGroupByCategory(
                    user.getId(), monthStart, monthEnd);
            BigDecimal totalInOut = totalIncome.add(totalExpense);
            List<DashboardResponse.CategoryStat> categoryStats = new ArrayList<>();
            for (Object[] row : categoryRaw) {
                String category = (String) row[0];
                BigDecimal amount = (BigDecimal) row[1];
                double percentage = totalInOut.compareTo(BigDecimal.ZERO) > 0
                        ? amount.divide(totalInOut, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0.0;
                categoryStats.add(new DashboardResponse.CategoryStat(category, amount, percentage));
            }

            // 近6月趋势
            List<Object[]> trendRaw = transactionRepository.monthlyTrend(user.getId(), sixMonthsAgo);
            List<DashboardResponse.MonthlyTrend> monthlyTrend = new ArrayList<>();
            for (Object[] row : trendRaw) {
                String month = (String) row[0];
                BigDecimal income = (BigDecimal) row[1];
                BigDecimal expense = (BigDecimal) row[2];
                monthlyTrend.add(new DashboardResponse.MonthlyTrend(month, income, expense));
            }

            DashboardResponse response = new DashboardResponse(totalIncome, totalExpense, balance, categoryStats, monthlyTrend);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
