package com.cyna.modules.dashboard.application.query.getdashboard;

public record DashboardMetricReadModel(
        long value,
        DashboardComparisonReadModel comparisons
) {
}
