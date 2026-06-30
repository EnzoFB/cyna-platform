package com.cyna.modules.dashboard.application.query.getdashboard;

import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetAdminDashboardQueryHandler implements QueryHandler<GetAdminDashboardQuery, AdminDashboardReadModel> {

    private final AdminDashboardReadModelAssembler readModelAssembler;

    public GetAdminDashboardQueryHandler(AdminDashboardReadModelAssembler readModelAssembler) {
        this.readModelAssembler = readModelAssembler;
    }

    @Override
    public AdminDashboardReadModel handle(GetAdminDashboardQuery query) {
        return readModelAssembler.buildDashboard(query.year());
    }
}
