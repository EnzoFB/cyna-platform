package com.cyna.modules.dashboard.application.query.getdashboard;

/**
 * One bar of the "sales over time" histogram. {@code label} is the ISO date
 * (yyyy-MM-dd) of the day, or of the Monday starting the week — the front
 * formats it for display.
 */
public record DashboardSalesPointReadModel(
        String label,
        long revenue,
        long salesCount
) {
}
