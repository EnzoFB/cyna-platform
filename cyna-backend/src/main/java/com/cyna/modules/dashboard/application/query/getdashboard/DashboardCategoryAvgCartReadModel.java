package com.cyna.modules.dashboard.application.query.getdashboard;

public record DashboardCategoryAvgCartReadModel(
        String category,
        long avgCartValue,
        long orderCount
) {
}
