package com.zorvyn.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated summary data returned by the dashboard endpoint.
 */
@Data
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    /** Category → total amount for all transactions */
    private Map<String, BigDecimal> categoryTotals;

    /** Recent N transactions */
    private List<TransactionResponse> recentTransactions;

    private List<MonthlyTrend> monthlyTrends;

    @Data
    @Builder
    public static class MonthlyTrend {
        private String month;            // "01-04-2026"
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
