package com.finance.controller;

import com.finance.dto.TransactionRequest;
import com.finance.dto.TransactionResponse;
import com.finance.model.TransactionType;
import com.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Financial transaction CRUD. ANALYST and ADMIN can read; only ADMIN can create/update/delete.")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create transaction", description = "ADMIN only.")
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    @Operation(
        summary = "Get all transactions",
        description = "Paginated list with optional filters. ANALYST sees own records; ADMIN sees all."
    )
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Keyword search on title/category/description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by type: CREDIT or DEBIT") @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by exact category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter from date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Filter to date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(transactionService.getAll(
                principal.getName(), keyword, type, category, dateFrom, dateTo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    @Operation(summary = "Get transaction by ID", description = "ANALYST sees only their own; ADMIN sees any.")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(transactionService.getById(id, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update transaction", description = "ADMIN only.")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            Principal principal) {
        return ResponseEntity.ok(transactionService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete transaction", description = "ADMIN only.")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        transactionService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
