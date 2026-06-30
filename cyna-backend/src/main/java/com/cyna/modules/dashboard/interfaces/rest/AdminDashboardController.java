package com.cyna.modules.dashboard.interfaces.rest;

import com.cyna.modules.dashboard.application.command.updategoaltarget.DashboardGoalKey;
import com.cyna.modules.dashboard.application.command.updategoaltarget.UpdateDashboardGoalTargetCommand;
import com.cyna.modules.dashboard.application.command.updatemonthlyrevenuegoal.UpdateDashboardMonthlyRevenueGoalsCommand;
import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardReadModel;
import com.cyna.modules.dashboard.application.query.getdashboard.DashboardYearReadModel;
import com.cyna.modules.dashboard.application.query.getdashboard.GetAdminDashboardQuery;
import com.cyna.modules.dashboard.interfaces.rest.dto.request.UpdateDashboardGoalTargetRequest;
import com.cyna.modules.dashboard.interfaces.rest.dto.request.UpdateDashboardMonthlyRevenueGoalsRequest;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Back-office KPIs and revenue goals (ADMIN only)")
public class AdminDashboardController {

    private final Mediator mediator;

    public AdminDashboardController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Get the admin dashboard",
            description = "Returns KPIs and revenue goals for the requested year (defaults to the current year when omitted).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardReadModel>> getDashboard(
            @RequestParam(required = false) Integer year
    ) {
        AdminDashboardReadModel result = mediator.send(new GetAdminDashboardQuery(year));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Update a yearly goal target",
            description = "Sets the target value for a single dashboard goal (identified by its goal key) for the given year.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goal target updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown goal key or invalid target value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    @PutMapping("/{year}/goals/target")
    public ResponseEntity<ApiResponse<DashboardYearReadModel>> updateGoalTarget(
            @PathVariable int year,
            @Valid @RequestBody UpdateDashboardGoalTargetRequest request
    ) {
        DashboardGoalKey goalKey;
        try {
            goalKey = DashboardGoalKey.fromApiValue(request.goalKey());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", ex.getMessage()));
        }

        Result<DashboardYearReadModel> result = mediator.send(new UpdateDashboardGoalTargetCommand(
                year,
                goalKey,
                request.targetValue()
        ));
        return mapYearUpdateResult(result);
    }

    @Operation(summary = "Update monthly revenue goals",
            description = "Sets the monthly revenue goal applied across the given year.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly revenue goals updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid revenue goal value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    @PutMapping("/{year}/goals/monthly-revenue")
    public ResponseEntity<ApiResponse<DashboardYearReadModel>> updateMonthlyRevenueGoals(
            @PathVariable int year,
            @Valid @RequestBody UpdateDashboardMonthlyRevenueGoalsRequest request
    ) {
        Result<DashboardYearReadModel> result = mediator.send(new UpdateDashboardMonthlyRevenueGoalsCommand(
                year,
                request.monthlyRevenueGoal()
        ));
        return mapYearUpdateResult(result);
    }

    private ResponseEntity<ApiResponse<DashboardYearReadModel>> mapYearUpdateResult(
            Result<DashboardYearReadModel> result
    ) {
        return result.fold(
                data -> ResponseEntity.ok(ApiResponse.success(data)),
                error -> ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", error))
        );
    }
}
