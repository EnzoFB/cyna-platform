package com.cyna.modules.dashboard.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateDashboardGoalTargetRequest(
        @NotBlank String goalKey,
        @Min(0) long targetValue
) {
}
