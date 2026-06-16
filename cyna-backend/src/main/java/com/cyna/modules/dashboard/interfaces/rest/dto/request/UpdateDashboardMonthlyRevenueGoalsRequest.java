package com.cyna.modules.dashboard.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateDashboardMonthlyRevenueGoalsRequest(
        @NotNull
        @Size(min = 1, max = 12)
        List<@NotNull @Min(0) Long> monthlyRevenueGoal
) {
}
