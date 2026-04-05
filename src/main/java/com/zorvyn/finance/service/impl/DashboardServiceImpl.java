package com.zorvyn.finance.service.impl;

import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import com.zorvyn.finance.dto.response.TransactionResponse;
import com.zorvyn.finance.model.TransactionType;
import com.zorvyn.finance.repository.TransactionRepository;
import com.zorvyn.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;

    /** Number of recent transactions to surface on the dashboard. */
    private static final int RECENT_LIMIT = 10;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(int monthsBack) {
        // ---- 1. Top-level totals ----------------------------------------
        BigDecimal totalIncome   = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

        // ---- 2. Category totals -------------------------------------------
        List<Object[]> rawCategories = transactionRepository.sumGroupedByCategory();
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        for (Object[] row : rawCategories) {
            String     category = (String)     row[0];
            BigDecimal total    = (BigDecimal) row[1];
            categoryTotals.put(category, total);
        }

        // ---- 3. Recent transactions ---------------------------------------
        List<TransactionResponse> recent = transactionRepository
                .findRecentTransactions(PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(TransactionResponse::from)
                .toList();

        // ---- 4. Monthly trends --------------------------------------------
        int clampedMonths = Math.max(1, Math.min(monthsBack, 24));
        LocalDate startDate = LocalDate.now()
                .withDayOfMonth(1)
                .minusMonths(clampedMonths - 1L);

        List<Object[]> rawTrends = transactionRepository.findMonthlyTrends(startDate);

        // Build a map: month -> {income, expense}
        Map<String, BigDecimal[]> trendMap = new LinkedHashMap<>();
        for (Object[] row : rawTrends) {
            String          month  = (String)          row[0];
            TransactionType type   = TransactionType.valueOf((String) row[1]);
            BigDecimal      amount = (BigDecimal)       row[2];

            trendMap.putIfAbsent(month, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] buckets = trendMap.get(month);
            if (type == TransactionType.INCOME) {
                buckets[0] = buckets[0].add(amount);
            } else {
                buckets[1] = buckets[1].add(amount);
            }
        }

        List<DashboardSummaryResponse.MonthlyTrend> monthlyTrends = trendMap.entrySet()
                .stream()
                .map(entry -> {
                    BigDecimal inc = entry.getValue()[0];
                    BigDecimal exp = entry.getValue()[1];
                    return DashboardSummaryResponse.MonthlyTrend.builder()
                            .month(entry.getKey())
                            .income(inc)
                            .expenses(exp)
                            .net(inc.subtract(exp))
                            .build();
                })
                .toList();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .recentTransactions(recent)
                .monthlyTrends(monthlyTrends)
                .build();
    }
}
