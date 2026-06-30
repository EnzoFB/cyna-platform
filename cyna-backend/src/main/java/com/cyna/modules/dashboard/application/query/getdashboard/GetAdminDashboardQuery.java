package com.cyna.modules.dashboard.application.query.getdashboard;

import com.cyna.shared.application.Query;

public record GetAdminDashboardQuery(Integer year) implements Query<AdminDashboardReadModel> {
}
