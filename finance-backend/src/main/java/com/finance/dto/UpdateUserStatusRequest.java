package com.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}
