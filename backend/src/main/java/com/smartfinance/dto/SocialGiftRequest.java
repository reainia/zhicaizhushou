package com.smartfinance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SocialGiftRequest {
    private String eventType;
    private String personName;
    private BigDecimal amount;
    private String direction; // GIVE / RECEIVE
    private LocalDate eventDate;
    private String description;
}
