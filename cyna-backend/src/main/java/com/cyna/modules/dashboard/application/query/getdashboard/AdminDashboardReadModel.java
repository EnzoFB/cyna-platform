package com.cyna.modules.dashboard.application.query.getdashboard;

import java.util.List;

public record AdminDashboardReadModel(
        int currentYear,
        List<Integer> availableYears,
        List<DashboardYearReadModel> years
) {
}
