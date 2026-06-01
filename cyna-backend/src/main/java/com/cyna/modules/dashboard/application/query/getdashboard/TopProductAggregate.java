package com.cyna.modules.dashboard.application.query.getdashboard;

import java.util.UUID;

public record TopProductAggregate(
        UUID productId,
        String name,
        long salesCount,
        long revenueAmount
) {
}
