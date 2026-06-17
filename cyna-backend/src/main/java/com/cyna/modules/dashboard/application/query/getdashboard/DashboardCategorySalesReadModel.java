package com.cyna.modules.dashboard.application.query.getdashboard;

public record DashboardCategorySalesReadModel(
        String category,
        long revenue,
        long quantity
) {
}
