package com.cyna.modules.dashboard.application.query.getdashboard;

public record DashboardComparisonReadModel(
        double week,
        long   weekValue,
        double month,
        long   monthValue,
        double quarter,
        long   quarterValue
) {
}
