package com.smartfinance.controller;

import com.smartfinance.dto.TransactionRequest;
import com.smartfinance.entity.Transaction;
import com.smartfinance.entity.User;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

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
    public ResponseEntity<?> listTransactions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            List<Transaction> transactions;

            if (year != null && month != null) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                transactions = transactionRepository.findByUserIdAndTransactionDateBetween(user.getId(), start, end);
            } else {
                transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
            }

            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TransactionRequest request) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            Transaction transaction = new Transaction();
            transaction.setUserId(user.getId());
            transaction.setAmount(request.getAmount());
            transaction.setType(request.getType());
            transaction.setCategory(request.getCategory());
            transaction.setTransactionDate(request.getTransactionDate());
            transaction.setDescription(request.getDescription());
            transaction.setCreatedAt(LocalDateTime.now());

            transactionRepository.save(transaction);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody TransactionRequest request) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            Transaction transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("交易记录不存在"));

            if (!transaction.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("无权修改此记录");
            }

            transaction.setAmount(request.getAmount());
            transaction.setType(request.getType());
            transaction.setCategory(request.getCategory());
            transaction.setTransactionDate(request.getTransactionDate());
            transaction.setDescription(request.getDescription());

            transactionRepository.save(transaction);
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            Transaction transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("交易记录不存在"));

            if (!transaction.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("无权删除此记录");
            }

            transactionRepository.delete(transaction);
            return ResponseEntity.ok("删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
