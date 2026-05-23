package com.smartfinance.repository;

import com.smartfinance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<Transaction> findTop5ByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByUserIdAndTypeAndDateBetween(@Param("userId") Long userId, @Param("type") String type, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.transactionDate BETWEEN :start AND :end GROUP BY t.category")
    List<Object[]> sumGroupByCategory(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = "SELECT DATE_FORMAT(t.transaction_date, '%Y-%m') as month, " +
           "COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) as income, " +
           "COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) as expense " +
           "FROM transactions t WHERE t.user_id = :userId AND t.transaction_date >= :startDate " +
           "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
}
