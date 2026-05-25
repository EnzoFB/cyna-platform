package com.cyna.modules.dashboard.application.command.updategoaltarget;

import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.shared.application.Command;

public record UpdateDashboardGoalTargetCommand(
        int year,
        DashboardGoalKey goalKey,
        long targetValue
) implements Command<DashboardYearReadModel> {
}
