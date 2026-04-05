package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.response.DashboardSummaryResponse;

public interface DashboardService {

    /**
     * Returns aggregated summary for the dashboard:
     * totals, category breakdown, recent activity, and monthly trends.
     *
     * @param monthsBack how many months of history to include in trends (default 6)
     */
    DashboardSummaryResponse getSummary(int monthsBack);
}
