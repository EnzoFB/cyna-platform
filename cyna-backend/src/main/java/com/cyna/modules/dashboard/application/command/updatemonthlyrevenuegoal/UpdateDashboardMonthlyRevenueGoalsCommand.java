package com.cyna.modules.dashboard.application.command.updatemonthlyrevenuegoal;

import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.shared.application.Command;

import java.util.List;

public record UpdateDashboardMonthlyRevenueGoalsCommand(
        int year,
        List<Long> monthlyRevenueGoal
) implements Command<DashboardYearReadModel> {
}
