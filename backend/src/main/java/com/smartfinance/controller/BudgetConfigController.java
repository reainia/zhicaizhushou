package com.smartfinance.controller;

import com.smartfinance.dto.BudgetConfigRequest;
import com.smartfinance.dto.BudgetSummaryResponse;
import com.smartfinance.entity.BudgetConfig;
import com.smartfinance.entity.User;
import com.smartfinance.repository.BudgetConfigRepository;
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
@RequestMapping("/api/budgets")
public class BudgetConfigController {

    @Autowired
    private BudgetConfigRepository budgetConfigRepository;

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

    @GetMapping
    public ResponseEntity<?> getBudgets(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            LocalDate now = LocalDate.now();
            int m = month != null ? month : now.getMonthValue();
            int y = year != null ? year : now.getYear();

            List<BudgetConfig> budgets = budgetConfigRepository.findByUserIdAndYearAndMonth(user.getId(), y, m);
            return ResponseEntity.ok(budgets);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getBudgetSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            LocalDate now = LocalDate.now();
            int m = month != null ? month : now.getMonthValue();
            int y = year != null ? year : now.getYear();
            LocalDate start = LocalDate.of(y, m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            // 预算配置
            List<BudgetConfig> budgets = budgetConfigRepository.findByUserIdAndYearAndMonth(user.getId(), y, m);

            // 当月总支出
            BigDecimal totalExpense = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                    user.getId(), "EXPENSE", start, end);

            // 分类实际支出
            List<Object[]> categoryRaw = transactionRepository.sumGroupByCategory(
                    user.getId(), start, end);
            List<BudgetSummaryResponse.CategoryActual> categoryActuals = new ArrayList<>();
            for (Object[] row : categoryRaw) {
                String category = (String) row[0];
                BigDecimal amount = (BigDecimal) row[1];
                categoryActuals.add(new BudgetSummaryResponse.CategoryActual(category, amount));
            }

            BudgetSummaryResponse response = new BudgetSummaryResponse(budgets, totalExpense, categoryActuals);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> saveBudget(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BudgetConfigRequest request) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            LocalDate now = LocalDate.now();
            int month = request.getMonth() != null ? request.getMonth() : now.getMonthValue();
            int year = request.getYear() != null ? request.getYear() : now.getYear();

            BudgetConfig budget;

            if (request.getId() != null) {
                // 更新已有预算
                budget = budgetConfigRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("预算配置不存在"));
                if (!budget.getUserId().equals(user.getId())) {
                    return ResponseEntity.status(403).body("无权修改此预算");
                }
            } else {
                // 检查是否已存在同类预算
                var existing = request.getCategory() == null
                        ? budgetConfigRepository.findByUserIdAndCategoryIsNullAndYearAndMonth(user.getId(), year, month)
                        : budgetConfigRepository.findByUserIdAndCategoryAndYearAndMonth(user.getId(), request.getCategory(), year, month);
                if (existing.isPresent()) {
                    budget = existing.get();
                } else {
                    budget = new BudgetConfig();
                    budget.setUserId(user.getId());
                    budget.setMonth(month);
                    budget.setYear(year);
                    budget.setCategory(request.getCategory());
                }
            }

            budget.setAmount(request.getAmount());
            budgetConfigRepository.save(budget);

            return ResponseEntity.ok(budget);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            BudgetConfig budget = budgetConfigRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("预算配置不存在"));
            if (!budget.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("无权删除此预算");
            }
            budgetConfigRepository.delete(budget);
            return ResponseEntity.ok("删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
