package com.cyna.modules.dashboard.infrastructure.persistence.repository;

import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import com.cyna.modules.dashboard.infrastructure.persistence.entity.DashboardGoalSettingsJpaEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaDashboardGoalSettingsRepositoryAdapter implements DashboardGoalSettingsRepository {

    private final SpringDataDashboardGoalSettingsRepository springRepository;

    public JpaDashboardGoalSettingsRepositoryAdapter(SpringDataDashboardGoalSettingsRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<DashboardGoalSettings> findByFiscalYear(int fiscalYear) {
        return springRepository.findById(fiscalYear)
                .map(this::toDomain);
    }

    @Override
    public void save(DashboardGoalSettings settings) {
        DashboardGoalSettingsJpaEntity entity = springRepository.findById(settings.fiscalYear())
                .orElseGet(DashboardGoalSettingsJpaEntity::new);

        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        entity.setFiscalYear(settings.fiscalYear());
        entity.setRevenueTargetValue(BigDecimal.valueOf(settings.revenueTargetValue()));
        entity.setClientsTargetValue(settings.clientsTargetValue());
        entity.setMonthlyRevenueGoal(List.copyOf(settings.monthlyRevenueGoal()));

        springRepository.save(entity);
    }

    private DashboardGoalSettings toDomain(DashboardGoalSettingsJpaEntity entity) {
        long revenueTarget = entity.getRevenueTargetValue() == null
                ? 0L
                : entity.getRevenueTargetValue().setScale(0, RoundingMode.HALF_UP).longValue();
        long clientsTarget = entity.getClientsTargetValue() == null ? 0L : entity.getClientsTargetValue();
        List<Long> monthly = entity.getMonthlyRevenueGoal() == null
                ? List.of()
                : entity.getMonthlyRevenueGoal();

        return new DashboardGoalSettings(
                entity.getFiscalYear(),
                revenueTarget,
                clientsTarget,
                monthly
        );
    }
}
