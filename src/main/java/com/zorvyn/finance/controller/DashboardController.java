package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import com.zorvyn.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Aggregated financial summary — all authenticated roles")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(
        summary = "Get dashboard summary [VIEWER, ANALYST, ADMIN]",
        description = """
            Returns:
            - Total income / expenses / net balance
            - Category-wise totals
            - Recent 10 transactions
            - Monthly income vs expense trends
            
            `monthsBack` controls how many months of trend data to return (1–24, default 6).
            """
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @Parameter(description = "Number of months to include in trend data (1–24)")
            @RequestParam(defaultValue = "6") int monthsBack) {

        DashboardSummaryResponse summary = dashboardService.getSummary(monthsBack);
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched", summary));
    }
}
