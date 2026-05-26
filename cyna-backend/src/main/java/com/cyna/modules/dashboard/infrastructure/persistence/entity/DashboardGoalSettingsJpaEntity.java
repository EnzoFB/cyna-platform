package com.cyna.modules.dashboard.infrastructure.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dashboard_goal_settings", schema = "dashboard_schema")
public class DashboardGoalSettingsJpaEntity {

    @Id
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "revenue_target_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal revenueTargetValue = BigDecimal.ZERO;

    @Column(name = "clients_target_value", nullable = false)
    private Long clientsTargetValue = 0L;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "monthly_revenue_goal", nullable = false, columnDefinition = "jsonb")
    private List<Long> monthlyRevenueGoal = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getRevenueTargetValue() {
        return revenueTargetValue;
    }

    public void setRevenueTargetValue(BigDecimal revenueTargetValue) {
        this.revenueTargetValue = revenueTargetValue;
    }

    public Long getClientsTargetValue() {
        return clientsTargetValue;
    }

    public void setClientsTargetValue(Long clientsTargetValue) {
        this.clientsTargetValue = clientsTargetValue;
    }

    public List<Long> getMonthlyRevenueGoal() {
        return monthlyRevenueGoal;
    }

    public void setMonthlyRevenueGoal(List<Long> monthlyRevenueGoal) {
        this.monthlyRevenueGoal = monthlyRevenueGoal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
