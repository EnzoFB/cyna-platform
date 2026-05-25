import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type DashboardComparisonPeriod = 'week' | 'month' | 'quarter';
export type DashboardTopProductsMode = 'sales' | 'revenue';
export type DashboardMetricKey = 'revenue' | 'clients' | 'sales' | 'activeSubscriptions';
export type DashboardGoalKey = 'revenue' | 'clients';

export interface DashboardMetricData {
  readonly value: number;
  readonly comparisons: Record<DashboardComparisonPeriod, number>;
}

export interface DashboardGoalData {
  readonly inProgressValue: number;
  readonly targetValue: number;
}

export interface DashboardTopProduct {
  readonly id: string;
  readonly name: string;
  readonly salesCount: number;
  readonly revenueAmount: number;
}

export interface DashboardYearData {
  readonly year: number;
  readonly metrics: Record<DashboardMetricKey, DashboardMetricData>;
  readonly revenueGoal: DashboardGoalData;
  readonly newClientsGoal: DashboardGoalData;
  readonly monthlyRevenueGoal: number[];
  readonly monthlyRevenueActual: number[];
  readonly topProducts: DashboardTopProduct[];
}

export interface DashboardSnapshot {
  readonly currentYear: number;
  readonly availableYears: number[];
  readonly years: DashboardYearData[];
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

interface DashboardApiResponse {
  currentYear: number;
  availableYears: number[];
  years: DashboardYearData[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly dataset = new Map<number, DashboardYearData>();

  currentYear = new Date().getFullYear();

  loadDashboard(year?: number) {
    let params = new HttpParams();
    if (year !== undefined) {
      params = params.set('year', year);
    }

    return this.http.get<ApiResponse<DashboardApiResponse>>(
      `${environment.apiUrl}/admin/dashboard`,
      { params }
    ).pipe(
      map(response => {
        const payload = response.data;
        this.currentYear = payload.currentYear;

        const normalizedYears = payload.years.map(item => this.normalizeYearData(item));
        for (const yearData of normalizedYears) {
          this.dataset.set(yearData.year, yearData);
        }

        return {
          currentYear: payload.currentYear,
          availableYears: [...payload.availableYears].sort((a, b) => a - b),
          years: normalizedYears.map(item => this.clone(item)),
        } as DashboardSnapshot;
      })
    );
  }

  getAvailableYears(): number[] {
    const years = Array.from(this.dataset.keys());
    if (!years.length) {
      return [this.currentYear];
    }
    return years.sort((a, b) => a - b);
  }

  getYearData(year: number): DashboardYearData {
    const fallback = this.dataset.get(year)
      ?? this.dataset.get(this.currentYear)
      ?? this.emptyYearData(year || this.currentYear);

    return this.clone(fallback);
  }

  updateMonthlyRevenueGoal(year: number, monthlyRevenueGoal: number[]) {
    const normalizedGoal = monthlyRevenueGoal
      .slice(0, 12)
      .map(value => (Number.isFinite(value) && value > 0 ? Math.round(value) : 0));

    while (normalizedGoal.length < 12) {
      normalizedGoal.push(0);
    }

    return this.http.put<ApiResponse<DashboardYearData>>(
      `${environment.apiUrl}/admin/dashboard/${year}/goals/monthly-revenue`,
      { monthlyRevenueGoal: normalizedGoal }
    ).pipe(map(response => this.storeYear(response.data)));
  }

  updateGoalTarget(year: number, goalKey: DashboardGoalKey, targetValue: number) {
    return this.http.put<ApiResponse<DashboardYearData>>(
      `${environment.apiUrl}/admin/dashboard/${year}/goals/target`,
      {
        goalKey,
        targetValue: Number.isFinite(targetValue) && targetValue > 0 ? Math.round(targetValue) : 0,
      }
    ).pipe(map(response => this.storeYear(response.data)));
  }

  private storeYear(rawYearData: DashboardYearData): DashboardYearData {
    const normalized = this.normalizeYearData(rawYearData);
    this.dataset.set(normalized.year, normalized);
    return this.clone(normalized);
  }

  private normalizeYearData(raw: DashboardYearData): DashboardYearData {
    const metrics = raw?.metrics ?? {} as Record<DashboardMetricKey, DashboardMetricData>;
    const ensureMetric = (key: DashboardMetricKey): DashboardMetricData => {
      const metric = metrics[key];
      const comparisons = metric?.comparisons;
      return {
        value: Number.isFinite(metric?.value) ? Math.round(metric.value) : 0,
        comparisons: {
          week: Number.isFinite(comparisons?.week) ? Number(comparisons.week) : 0,
          month: Number.isFinite(comparisons?.month) ? Number(comparisons.month) : 0,
          quarter: Number.isFinite(comparisons?.quarter) ? Number(comparisons.quarter) : 0,
        },
      };
    };

    const monthlyRevenueGoal = this.normalizeMonthly(raw?.monthlyRevenueGoal);
    const monthlyRevenueActual = this.normalizeMonthly(raw?.monthlyRevenueActual);

    return {
      year: Number.isFinite(raw?.year) ? Math.trunc(raw.year) : this.currentYear,
      metrics: {
        revenue: ensureMetric('revenue'),
        clients: ensureMetric('clients'),
        sales: ensureMetric('sales'),
        activeSubscriptions: ensureMetric('activeSubscriptions'),
      },
      revenueGoal: {
        inProgressValue: Number.isFinite(raw?.revenueGoal?.inProgressValue)
          ? Math.round(raw.revenueGoal.inProgressValue)
          : 0,
        targetValue: Number.isFinite(raw?.revenueGoal?.targetValue)
          ? Math.round(raw.revenueGoal.targetValue)
          : 0,
      },
      newClientsGoal: {
        inProgressValue: Number.isFinite(raw?.newClientsGoal?.inProgressValue)
          ? Math.round(raw.newClientsGoal.inProgressValue)
          : 0,
        targetValue: Number.isFinite(raw?.newClientsGoal?.targetValue)
          ? Math.round(raw.newClientsGoal.targetValue)
          : 0,
      },
      monthlyRevenueGoal,
      monthlyRevenueActual,
      topProducts: (raw?.topProducts ?? []).map(product => ({
        id: product?.id ?? '',
        name: product?.name ?? '',
        salesCount: Number.isFinite(product?.salesCount) ? Math.round(product.salesCount) : 0,
        revenueAmount: Number.isFinite(product?.revenueAmount) ? Math.round(product.revenueAmount) : 0,
      })),
    };
  }

  private normalizeMonthly(values: number[] | undefined): number[] {
    const normalized = (values ?? [])
      .slice(0, 12)
      .map(value => (Number.isFinite(value) && value > 0 ? Math.round(value) : 0));

    while (normalized.length < 12) {
      normalized.push(0);
    }

    return normalized;
  }

  private emptyYearData(year: number): DashboardYearData {
    return {
      year,
      metrics: {
        revenue: { value: 0, comparisons: { week: 0, month: 0, quarter: 0 } },
        clients: { value: 0, comparisons: { week: 0, month: 0, quarter: 0 } },
        sales: { value: 0, comparisons: { week: 0, month: 0, quarter: 0 } },
        activeSubscriptions: { value: 0, comparisons: { week: 0, month: 0, quarter: 0 } },
      },
      revenueGoal: { inProgressValue: 0, targetValue: 0 },
      newClientsGoal: { inProgressValue: 0, targetValue: 0 },
      monthlyRevenueGoal: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      monthlyRevenueActual: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      topProducts: [],
    };
  }

  private clone(data: DashboardYearData): DashboardYearData {
    return {
      ...data,
      metrics: {
        revenue: { ...data.metrics.revenue, comparisons: { ...data.metrics.revenue.comparisons } },
        clients: { ...data.metrics.clients, comparisons: { ...data.metrics.clients.comparisons } },
        sales: { ...data.metrics.sales, comparisons: { ...data.metrics.sales.comparisons } },
        activeSubscriptions: {
          ...data.metrics.activeSubscriptions,
          comparisons: { ...data.metrics.activeSubscriptions.comparisons },
        },
      },
      revenueGoal: { ...data.revenueGoal },
      newClientsGoal: { ...data.newClientsGoal },
      monthlyRevenueGoal: [...data.monthlyRevenueGoal],
      monthlyRevenueActual: [...data.monthlyRevenueActual],
      topProducts: data.topProducts.map(product => ({ ...product })),
    };
  }
}
