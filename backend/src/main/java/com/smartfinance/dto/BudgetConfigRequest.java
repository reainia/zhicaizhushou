package com.smartfinance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetConfigRequest {
    private Long id;
    private String category; // null = 整体预算
    private BigDecimal amount;
    private Integer month;
    private Integer year;
}
