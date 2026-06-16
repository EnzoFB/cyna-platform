package com.cyna.modules.dashboard.infrastructure.persistence.repository;

import com.cyna.modules.dashboard.infrastructure.persistence.entity.DashboardGoalSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDashboardGoalSettingsRepository extends JpaRepository<DashboardGoalSettingsJpaEntity, Integer> {
}
