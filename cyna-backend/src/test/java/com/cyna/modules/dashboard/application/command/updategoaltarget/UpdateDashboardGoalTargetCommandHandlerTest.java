package com.cyna.modules.dashboard.application.command.updategoaltarget;

import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardReadModelAssembler;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardComparisonReadModel;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardGoalReadModel;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardMetricReadModel;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDashboardGoalTargetCommandHandlerTest {

    @Mock
    private DashboardGoalSettingsRepository goalSettingsRepository;

    @Mock
    private AdminDashboardReadModelAssembler readModelAssembler;

    private UpdateDashboardGoalTargetCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) {
            action.run();
        }

        @Override
        public <T> T runReturning(Supplier<T> action) {
            return action.get();
        }
    };

    @BeforeEach
    void setUp() {
        handler = new UpdateDashboardGoalTargetCommandHandler(
                goalSettingsRepository,
                readModelAssembler,
                transactionRunner
        );
    }

    @Test
    void should_update_revenue_target_and_rescale_monthly_goal() {
        int year = 2026;
        DashboardYearReadModel initial = baselineYear(year, 1_000L, 120L, List.of(
                80L, 80L, 80L, 80L, 80L, 80L, 80L, 80L, 80L, 80L, 80L, 120L
        ));
        DashboardYearReadModel updated = baselineYear(year, 2_000L, 120L, List.of(
                160L, 160L, 160L, 160L, 160L, 160L, 160L, 160L, 160L, 160L, 160L, 240L
        ));

        when(readModelAssembler.buildYear(year)).thenReturn(initial, updated);

        Result<DashboardYearReadModel> result = handler.handle(new UpdateDashboardGoalTargetCommand(
                year,
                DashboardGoalKey.REVENUE,
                2_000L
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().revenueGoal().targetValue()).isEqualTo(2_000L);

        ArgumentCaptor<com.cyna.modules.dashboard.domain.model.DashboardGoalSettings> captor =
                ArgumentCaptor.forClass(com.cyna.modules.dashboard.domain.model.DashboardGoalSettings.class);
        verify(goalSettingsRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.fiscalYear()).isEqualTo(year);
        assertThat(saved.revenueTargetValue()).isEqualTo(2_000L);
        assertThat(saved.monthlyRevenueGoal().stream().mapToLong(Long::longValue).sum()).isEqualTo(2_000L);
    }

    @Test
    void should_return_failure_when_target_is_negative() {
        Result<DashboardYearReadModel> result = handler.handle(new UpdateDashboardGoalTargetCommand(
                2026,
                DashboardGoalKey.CLIENTS,
                -1L
        ));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("non-negative");
    }

    private DashboardYearReadModel baselineYear(int year, long revenueTarget, long clientsTarget, List<Long> monthlyGoal) {
        var empty = new DashboardComparisonReadModel(0d, 0L, 0d, 0L, 0d, 0L);
        Map<String, DashboardMetricReadModel> metrics = Map.of(
                "revenue",             new DashboardMetricReadModel(0L, empty),
                "clients",             new DashboardMetricReadModel(0L, empty),
                "sales",               new DashboardMetricReadModel(0L, empty),
                "activeSubscriptions", new DashboardMetricReadModel(0L, empty)
        );
        return new DashboardYearReadModel(
                year,
                metrics,
                new DashboardGoalReadModel(0L, revenueTarget),
                new DashboardGoalReadModel(0L, clientsTarget),
                monthlyGoal,
                List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
