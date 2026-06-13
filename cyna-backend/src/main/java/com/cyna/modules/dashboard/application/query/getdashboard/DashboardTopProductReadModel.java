package com.cyna.modules.dashboard.application.query.getdashboard;

public record DashboardTopProductReadModel(
        String id,
        String name,
        long salesCount,
        long revenueAmount
) {
}
