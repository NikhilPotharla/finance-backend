package com.finance.controller;

import com.finance.dto.CategoryTotalResponse;
import com.finance.dto.DashboardSummaryResponse;
import com.finance.dto.MonthlyTrendResponse;
import com.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Analytics and summary endpoints — available to VIEWER, ANALYST, and ADMIN")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @Operation(
        summary = "Get dashboard summary",
        description = "Returns total income, total expenses, net balance, transaction count, category count, " +
                      "category breakdown, and 10 most recent transactions. " +
                      "ADMIN sees data for all users; others see their own data."
    )
    public ResponseEntity<DashboardSummaryResponse> getSummary(Principal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal.getName()));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @Operation(
        summary = "Category breakdown",
        description = "Returns total amount and percentage share per category. " +
                      "ADMIN sees system-wide; others see their own."
    )
    public ResponseEntity<List<CategoryTotalResponse>> getCategoryBreakdown(Principal principal) {
        return ResponseEntity.ok(dashboardService.getCategoryBreakdown(principal.getName()));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    @Operation(
        summary = "Monthly trends (last 6 months)",
        description = "Returns month-by-month income, expenses, and net for the last 6 months. " +
                      "ANALYST and ADMIN only."
    )
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrends(Principal principal) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends(principal.getName()));
    }
}
