package com.cyna.modules.dashboard.application.query.getdashboard;

import java.time.LocalDate;

public record DailyRevenueAggregate(
        LocalDate date,
        long revenueAmount,
        long salesCount
) {
}
