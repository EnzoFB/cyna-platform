package com.cyna.modules.dashboard.application.query.getdashboard;

import java.util.List;
import java.util.Map;

public record DashboardYearReadModel(
        int year,
        Map<String, DashboardMetricReadModel> metrics,
        DashboardGoalReadModel revenueGoal,
        DashboardGoalReadModel newClientsGoal,
        List<Long> monthlyRevenueGoal,
        List<Long> monthlyRevenueActual,
        List<DashboardTopProductReadModel> topProducts,
        Map<String, Long> ordersByStatus,
        List<DashboardSalesPointReadModel> dailySales,
        List<DashboardSalesPointReadModel> weeklySales,
        List<DashboardCategoryAvgCartReadModel> categoryAvgCart,
        List<DashboardCategorySalesReadModel> categorySales,
        List<DashboardCategoryAvgCartReadModel> categoryAvgCartDaily,
        List<DashboardCategoryAvgCartReadModel> categoryAvgCartWeekly,
        List<DashboardCategorySalesReadModel> categorySalesDaily,
        List<DashboardCategorySalesReadModel> categorySalesWeekly
) {
}
