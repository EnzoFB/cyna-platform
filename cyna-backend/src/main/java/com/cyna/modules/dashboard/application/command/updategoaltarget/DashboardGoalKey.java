package com.cyna.modules.dashboard.application.command.updategoaltarget;

public enum DashboardGoalKey {
    REVENUE,
    CLIENTS;

    public static DashboardGoalKey fromApiValue(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Goal key is required");
        }
        return switch (rawValue.trim().toLowerCase()) {
            case "revenue" -> REVENUE;
            case "clients" -> CLIENTS;
            default -> throw new IllegalArgumentException("Unsupported goal key: " + rawValue);
        };
    }
}
