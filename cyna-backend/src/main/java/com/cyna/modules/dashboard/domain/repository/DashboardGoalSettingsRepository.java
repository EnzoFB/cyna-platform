package com.cyna.modules.dashboard.domain.repository;

import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;

import java.util.Optional;

public interface DashboardGoalSettingsRepository {

    Optional<DashboardGoalSettings> findByFiscalYear(int fiscalYear);

    void save(DashboardGoalSettings settings);
}
