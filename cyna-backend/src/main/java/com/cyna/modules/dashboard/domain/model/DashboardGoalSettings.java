package com.cyna.modules.dashboard.domain.model;

import java.util.ArrayList;
import java.util.List;

public record DashboardGoalSettings(
        int fiscalYear,
        long revenueTargetValue,
        long clientsTargetValue,
        List<Long> monthlyRevenueGoal
) {

    public DashboardGoalSettings {
        if (fiscalYear <= 0) {
            throw new IllegalArgumentException("Fiscal year must be positive");
        }
        if (revenueTargetValue < 0) {
            throw new IllegalArgumentException("Revenue target must be non-negative");
        }
        if (clientsTargetValue < 0) {
            throw new IllegalArgumentException("Clients target must be non-negative");
        }
        monthlyRevenueGoal = normalizeMonthlyGoal(monthlyRevenueGoal);
    }

    public DashboardGoalSettings withRevenueTargetValue(long targetValue) {
        return new DashboardGoalSettings(
                fiscalYear,
                Math.max(0, targetValue),
                clientsTargetValue,
                scaleMonthlyGoalToTarget(monthlyRevenueGoal, Math.max(0, targetValue))
        );
    }

    public DashboardGoalSettings withClientsTargetValue(long targetValue) {
        return new DashboardGoalSettings(
                fiscalYear,
                revenueTargetValue,
                Math.max(0, targetValue),
                monthlyRevenueGoal
        );
    }

    public DashboardGoalSettings withMonthlyRevenueGoal(List<Long> monthlyGoal) {
        List<Long> normalized = normalizeMonthlyGoal(monthlyGoal);
        long computedTarget = normalized.stream().mapToLong(Long::longValue).sum();
        return new DashboardGoalSettings(
                fiscalYear,
                computedTarget,
                clientsTargetValue,
                normalized
        );
    }

    public static List<Long> normalizeMonthlyGoal(List<Long> monthlyGoal) {
        List<Long> normalized = new ArrayList<>(12);
        if (monthlyGoal != null) {
            for (Long value : monthlyGoal) {
                normalized.add(Math.max(0L, value == null ? 0L : value));
                if (normalized.size() == 12) {
                    break;
                }
            }
        }
        while (normalized.size() < 12) {
            normalized.add(0L);
        }
        return List.copyOf(normalized);
    }

    private static List<Long> scaleMonthlyGoalToTarget(List<Long> monthlyGoal, long targetValue) {
        List<Long> normalized = normalizeMonthlyGoal(monthlyGoal);
        long currentTotal = normalized.stream().mapToLong(Long::longValue).sum();

        if (targetValue == 0) {
            return List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        if (currentTotal <= 0) {
            return distributeEvenly(targetValue);
        }

        double scale = (double) targetValue / (double) currentTotal;
        List<Long> scaled = new ArrayList<>(normalized.size());
        for (Long monthValue : normalized) {
            scaled.add(Math.max(0L, Math.round(monthValue * scale)));
        }

        long scaledTotal = scaled.stream().mapToLong(Long::longValue).sum();
        long delta = targetValue - scaledTotal;
        int lastIndex = scaled.size() - 1;
        scaled.set(lastIndex, Math.max(0L, scaled.get(lastIndex) + delta));

        return List.copyOf(scaled);
    }

    public static List<Long> distributeEvenly(long targetValue) {
        if (targetValue <= 0) {
            return List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        long base = targetValue / 12L;
        long remainder = targetValue - (base * 12L);
        List<Long> values = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            if (i == 11) {
                values.add(base + remainder);
            } else {
                values.add(base);
            }
        }
        return List.copyOf(values);
    }
}
