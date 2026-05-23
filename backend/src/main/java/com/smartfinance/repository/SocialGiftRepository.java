package com.smartfinance.repository;

import com.smartfinance.entity.SocialGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SocialGiftRepository extends JpaRepository<SocialGift, Long> {
    List<SocialGift> findByUserIdOrderByEventDateDesc(Long userId);

    List<SocialGift> findByUserIdAndEventDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM SocialGift s WHERE s.userId = :userId AND s.direction = :direction AND s.eventDate BETWEEN :start AND :end")
    BigDecimal sumByUserIdAndDirectionAndDateBetween(@Param("userId") Long userId, @Param("direction") String direction, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
