package com.cyna.modules.dashboard.application.query.getdashboard;

import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardReadModelAssemblerTest {

    @Mock
    private AdminDashboardQueryPort queryPort;

    @Mock
    private DashboardGoalSettingsRepository goalSettingsRepository;

    private AdminDashboardReadModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new AdminDashboardReadModelAssembler(queryPort, goalSettingsRepository);
    }

    @Test
    void should_build_dashboard_with_defaults_when_no_saved_goal_settings() {
        int currentYear = Year.now(ZoneId.of("Europe/Paris")).getValue();
        UUID productId = UUID.randomUUID();

        when(queryPort.findAvailableYears()).thenReturn(List.of(currentYear - 1));
        when(queryPort.sumRevenueBetween(any(), any())).thenReturn(1_000L);
        when(queryPort.sumSalesQuantityBetween(any(), any())).thenReturn(250L);
        when(queryPort.countCustomersCreatedBefore(any())).thenReturn(5_000L);
        when(queryPort.countCustomersCreatedBetween(any(), any())).thenReturn(120L);
        when(queryPort.countActiveSubscriptionsAt(any())).thenReturn(80L);
        when(queryPort.findMonthlyRevenueByYear(anyInt())).thenReturn(List.of(
                new MonthlyRevenueAggregate(1, 120L),
                new MonthlyRevenueAggregate(2, 180L)
        ));
        when(queryPort.findTopProductsByYear(anyInt(), anyString(), anyInt())).thenReturn(List.of(
                new TopProductAggregate(productId, "SOC CYNA", 120L, 24000L)
        ));
        when(goalSettingsRepository.findByFiscalYear(anyInt())).thenReturn(Optional.empty());

        AdminDashboardReadModel result = assembler.buildDashboard(currentYear);

        assertThat(result.currentYear()).isEqualTo(currentYear);
        assertThat(result.availableYears()).contains(currentYear - 1, currentYear);
        assertThat(result.years()).hasSize(1);

        DashboardYearReadModel yearData = result.years().getFirst();
        assertThat(yearData.year()).isEqualTo(currentYear);
        assertThat(yearData.metrics().get("revenue").value()).isEqualTo(1_000L);
        assertThat(yearData.metrics().get("sales").value()).isEqualTo(250L);
        assertThat(yearData.metrics().get("clients").value()).isEqualTo(5_000L);
        assertThat(yearData.metrics().get("activeSubscriptions").value()).isEqualTo(80L);
        assertThat(yearData.revenueGoal().inProgressValue()).isEqualTo(1_000L);
        assertThat(yearData.revenueGoal().targetValue()).isEqualTo(1_200L);
        assertThat(yearData.newClientsGoal().inProgressValue()).isEqualTo(120L);
        assertThat(yearData.newClientsGoal().targetValue()).isEqualTo(144L);
        assertThat(yearData.monthlyRevenueGoal()).hasSize(12);
        assertThat(yearData.monthlyRevenueGoal().stream().mapToLong(Long::longValue).sum()).isEqualTo(1_200L);
        assertThat(yearData.monthlyRevenueActual()).hasSize(12);
        assertThat(yearData.monthlyRevenueActual().get(0)).isEqualTo(120L);
        assertThat(yearData.monthlyRevenueActual().get(1)).isEqualTo(180L);
        assertThat(yearData.topProducts()).hasSize(1);
        assertThat(yearData.topProducts().getFirst().id()).isEqualTo(productId.toString());
    }

    @Test
    void should_apply_saved_goal_settings_when_present() {
        int year = 2025;
        List<Long> monthlyGoal = List.of(400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 400L, 600L);

        when(queryPort.sumRevenueBetween(any(), any())).thenReturn(2_300L);
        when(queryPort.sumSalesQuantityBetween(any(), any())).thenReturn(90L);
        when(queryPort.countCustomersCreatedBefore(any())).thenReturn(4_000L);
        when(queryPort.countCustomersCreatedBetween(any(), any())).thenReturn(80L);
        when(queryPort.countActiveSubscriptionsAt(any())).thenReturn(65L);
        when(queryPort.findMonthlyRevenueByYear(anyInt())).thenReturn(List.of());
        when(queryPort.findTopProductsByYear(anyInt(), anyString(), anyInt())).thenReturn(List.of());
        when(goalSettingsRepository.findByFiscalYear(year)).thenReturn(Optional.of(
                new DashboardGoalSettings(year, 5_000L, 350L, monthlyGoal)
        ));

        DashboardYearReadModel yearData = assembler.buildYear(year);

        assertThat(yearData.revenueGoal().targetValue()).isEqualTo(5_000L);
        assertThat(yearData.newClientsGoal().targetValue()).isEqualTo(350L);
        assertThat(yearData.monthlyRevenueGoal()).containsExactlyElementsOf(monthlyGoal);
    }
}
