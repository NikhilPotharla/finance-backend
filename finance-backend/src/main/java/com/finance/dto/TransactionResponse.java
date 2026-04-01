package com.finance.dto;

import com.finance.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String title;
    private BigDecimal amount;
    private TransactionType type;
    private String category;
    private LocalDate date;
    private String description;
    private String ownerEmail;
    private String ownerName;
}
