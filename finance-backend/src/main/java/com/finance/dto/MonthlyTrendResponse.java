package com.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendResponse {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal net;
}
