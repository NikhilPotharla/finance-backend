package com.finance.dto;

import com.finance.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;
}
