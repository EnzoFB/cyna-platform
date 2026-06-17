package com.cyna.modules.dashboard.application.query.getdashboard;

public record CategoryAvgCartAggregate(
        String category,
        long avgCartValue,
        long orderCount
) {
}
