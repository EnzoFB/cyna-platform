package com.cyna.modules.dashboard.application.command.updatemonthlyrevenuegoal;

import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardReadModelAssembler;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateDashboardMonthlyRevenueGoalsCommandHandler implements CommandHandler<UpdateDashboardMonthlyRevenueGoalsCommand, DashboardYearReadModel> {

    private final DashboardGoalSettingsRepository goalSettingsRepository;
    private final AdminDashboardReadModelAssembler readModelAssembler;
    private final TransactionRunner transactionRunner;

    public UpdateDashboardMonthlyRevenueGoalsCommandHandler(
            DashboardGoalSettingsRepository goalSettingsRepository,
            AdminDashboardReadModelAssembler readModelAssembler,
            TransactionRunner transactionRunner
    ) {
        this.goalSettingsRepository = goalSettingsRepository;
        this.readModelAssembler = readModelAssembler;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<DashboardYearReadModel> handle(UpdateDashboardMonthlyRevenueGoalsCommand command) {
        if (command.year() <= 0) {
            return Result.failure("Invalid fiscal year");
        }
        if (command.monthlyRevenueGoal() == null) {
            return Result.failure("Monthly revenue goals are required");
        }

        return transactionRunner.runReturning(() -> {
            DashboardYearReadModel current = readModelAssembler.buildYear(command.year());
            DashboardGoalSettings baseSettings = new DashboardGoalSettings(
                    command.year(),
                    current.revenueGoal().targetValue(),
                    current.newClientsGoal().targetValue(),
                    current.monthlyRevenueGoal()
            );

            DashboardGoalSettings updatedSettings = baseSettings.withMonthlyRevenueGoal(command.monthlyRevenueGoal());
            goalSettingsRepository.save(updatedSettings);
            return Result.success(readModelAssembler.buildYear(command.year()));
        });
    }
}
