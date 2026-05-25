package com.cyna.modules.dashboard.application.command.updategoaltarget;

import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardReadModelAssembler;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateDashboardGoalTargetCommandHandler implements CommandHandler<UpdateDashboardGoalTargetCommand, DashboardYearReadModel> {

    private final DashboardGoalSettingsRepository goalSettingsRepository;
    private final AdminDashboardReadModelAssembler readModelAssembler;
    private final TransactionRunner transactionRunner;

    public UpdateDashboardGoalTargetCommandHandler(
            DashboardGoalSettingsRepository goalSettingsRepository,
            AdminDashboardReadModelAssembler readModelAssembler,
            TransactionRunner transactionRunner
    ) {
        this.goalSettingsRepository = goalSettingsRepository;
        this.readModelAssembler = readModelAssembler;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<DashboardYearReadModel> handle(UpdateDashboardGoalTargetCommand command) {
        if (command.year() <= 0) {
            return Result.failure("Invalid fiscal year");
        }
        if (command.goalKey() == null) {
            return Result.failure("Goal key is required");
        }
        if (command.targetValue() < 0) {
            return Result.failure("Goal target must be non-negative");
        }

        return transactionRunner.runReturning(() -> {
            DashboardYearReadModel current = readModelAssembler.buildYear(command.year());
            DashboardGoalSettings baseSettings = new DashboardGoalSettings(
                    command.year(),
                    current.revenueGoal().targetValue(),
                    current.newClientsGoal().targetValue(),
                    current.monthlyRevenueGoal()
            );

            DashboardGoalSettings updatedSettings = switch (command.goalKey()) {
                case REVENUE -> baseSettings.withRevenueTargetValue(command.targetValue());
                case CLIENTS -> baseSettings.withClientsTargetValue(command.targetValue());
            };

            goalSettingsRepository.save(updatedSettings);
            return Result.success(readModelAssembler.buildYear(command.year()));
        });
    }
}
