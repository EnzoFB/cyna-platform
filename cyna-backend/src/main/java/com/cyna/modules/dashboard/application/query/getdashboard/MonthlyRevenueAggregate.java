package com.cyna.modules.dashboard.application.query.getdashboard;

public record MonthlyRevenueAggregate(
        int month,
        long revenueAmount
) {
}
