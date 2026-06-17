package com.cyna.modules.dashboard.application.query.getdashboard;

public record CategorySalesAggregate(
        String category,
        long revenueAmount,
        long quantity
) {
}
