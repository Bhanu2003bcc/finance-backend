package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.request.TransactionFilterRequest;
import com.zorvyn.finance.dto.request.TransactionRequest;
import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.TransactionResponse;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Financial record management — CRUD + filtering")
public class TransactionController {

    private final TransactionService transactionService;

    // Create — ANALYST, ADMIN

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Create a new financial record [ANALYST, ADMIN]")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {

        TransactionResponse created = transactionService.createTransaction(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", created));
    }

    // Read — VIEWER, ANALYST, ADMIN

    @GetMapping
    @Operation(summary = "List transactions with optional filters [ALL ROLES]",
               description = "Supports filtering by type, category, date range, and keyword search. " +
                             "Results are paginated (default page=0, size=20).")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAll(
            @Parameter(description = "Filter by type: INCOME or EXPENSE")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by category (case-insensitive, exact match)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Start date (inclusive) — format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (inclusive) — format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Keyword search in notes and category")
            @RequestParam(required = false) String keyword,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setCategory(category);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setKeyword(keyword);
        filter.setPage(page);
        filter.setSize(size);

        // Parse type safely
        if (type != null && !type.isBlank()) {
            try {
                filter.setType(
                    com.zorvyn.finance.model.TransactionType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new com.zorvyn.finance.exception.AppExceptions.BadRequestException(
                    "Invalid type value '" + type + "'. Use INCOME or EXPENSE.");
            }
        }

        Page<TransactionResponse> result = transactionService.getTransactions(filter);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by ID [ALL ROLES]")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(transactionService.getTransactionById(id)));
    }

    // Update — ANALYST (own), ADMIN (any)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Update a transaction [ANALYST (own), ADMIN (any)]")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {

        TransactionResponse updated = transactionService.updateTransaction(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", updated));
    }

    // Delete (soft) — ANALYST (own), ADMIN (any)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Soft-delete a transaction [ANALYST (own), ADMIN (any)]",
               description = "Records are not physically removed — they are flagged as deleted " +
                             "to preserve the audit trail.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        transactionService.deleteTransaction(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }

    @GetMapping("/export")
    @Operation(summary = "Export transactions to CSV [ALL ROLES]",
            description = "Downloads a CSV file of transactions based on provided filters.")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) {

        // Build the filter request (ignoring pagination)
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setCategory(category);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setKeyword(keyword);

        if (type != null && !type.isBlank()) {
            try {
                filter.setType(com.zorvyn.finance.model.TransactionType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new com.zorvyn.finance.exception.AppExceptions.BadRequestException(
                        "Invalid type value '" + type + "'. Use INCOME or EXPENSE.");
            }
        }

        // Generate the CSV binary data
        byte[] csvData = transactionService.exportTransactionsToCsv(filter);

        // Configure headers to trigger a file download in the client
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_export.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}
