package com.smartfinance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    private BigDecimal amount;
    private String type;
    private String category;
    private LocalDate transactionDate;
    private String description;
}
