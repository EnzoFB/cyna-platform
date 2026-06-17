package com.cyna.modules.dashboard.application.query.getdashboard;

import java.time.LocalDate;

public record WeeklyRevenueAggregate(
        LocalDate weekStart,
        long revenueAmount,
        long salesCount
) {
}
