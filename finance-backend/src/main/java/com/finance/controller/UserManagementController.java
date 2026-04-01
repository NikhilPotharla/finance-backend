package com.finance.controller;

import com.finance.dto.UpdateUserRoleRequest;
import com.finance.dto.UpdateUserStatusRequest;
import com.finance.dto.UserResponse;
import com.finance.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin-only endpoints to manage users, roles, and account status")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "List all users", description = "Returns all registered users with their roles and status.")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userManagementService.getUserById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(
        summary = "Update user active status",
        description = "Activate or deactivate a user account. Deactivated users cannot log in."
    )
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userManagementService.updateStatus(id, request));
    }

    @PutMapping("/{id}/role")
    @Operation(
        summary = "Update user role",
        description = "Change a user's role to VIEWER, ANALYST, or ADMIN."
    )
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(userManagementService.updateRole(id, request));
    }
}
