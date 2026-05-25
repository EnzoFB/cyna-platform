import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import {
  DashboardComparisonPeriod,
  DashboardGoalKey,
  DashboardMetricKey,
  DashboardService,
  DashboardTopProductsMode,
} from '../../services/dashboard.service';

interface MetricCardViewModel {
  readonly key: DashboardMetricKey;
  readonly titleKey: string;
  readonly value: number;
  readonly comparisonValue: number;
  readonly period: DashboardComparisonPeriod;
}

interface TopProductViewModel {
  readonly id: string;
  readonly rank: number;
  readonly name: string;
  readonly percentage: number;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPageComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly months = [
    'dashboard.months.jan',
    'dashboard.months.feb',
    'dashboard.months.mar',
    'dashboard.months.apr',
    'dashboard.months.may',
    'dashboard.months.jun',
    'dashboard.months.jul',
    'dashboard.months.aug',
    'dashboard.months.sep',
    'dashboard.months.oct',
    'dashboard.months.nov',
    'dashboard.months.dec',
  ] as const;

  protected readonly availableYears = signal(this.dashboardService.getAvailableYears());
  protected readonly currentYear = signal(this.dashboardService.currentYear);
  protected readonly yearData = signal(this.dashboardService.getYearData(this.currentYear()));

  protected readonly selectedComparisonByMetric = signal<Record<DashboardMetricKey, DashboardComparisonPeriod>>({
    revenue: 'week',
    clients: 'week',
    sales: 'week',
    activeSubscriptions: 'week',
  });

  protected readonly openComparisonMenu = signal<DashboardMetricKey | null>(null);
  protected readonly topProductsMode = signal<DashboardTopProductsMode>('sales');
  protected readonly topProductsMenuOpen = signal(false);
  protected readonly revenueGoalConfigOpen = signal(false);
  protected readonly revenueGoalDraft = signal<number[]>([]);
  protected readonly sectionGoalConfigOpen = signal(false);
  protected readonly sectionGoalConfigType = signal<DashboardGoalKey | null>(null);
  protected readonly sectionGoalTargetDraft = signal(0);

  protected readonly metricCards = computed<MetricCardViewModel[]>(() => {
    const data = this.yearData().metrics;
    const selected = this.selectedComparisonByMetric();

    return [
      {
        key: 'revenue',
        titleKey: 'dashboard.metrics.revenue',
        value: data.revenue.value,
        comparisonValue: data.revenue.comparisons[selected.revenue],
        period: selected.revenue,
      },
      {
        key: 'clients',
        titleKey: 'dashboard.metrics.clients',
        value: data.clients.value,
        comparisonValue: data.clients.comparisons[selected.clients],
        period: selected.clients,
      },
      {
        key: 'sales',
        titleKey: 'dashboard.metrics.sales',
        value: data.sales.value,
        comparisonValue: data.sales.comparisons[selected.sales],
        period: selected.sales,
      },
      {
        key: 'activeSubscriptions',
        titleKey: 'dashboard.metrics.activeSubscriptions',
        value: data.activeSubscriptions.value,
        comparisonValue: data.activeSubscriptions.comparisons[selected.activeSubscriptions],
        period: selected.activeSubscriptions,
      },
    ];
  });

  protected readonly revenueProgressPct = computed(() => this.computeProgress(
    this.yearData().revenueGoal.inProgressValue,
    this.yearData().revenueGoal.targetValue
  ));

  protected readonly newClientsProgressPct = computed(() => this.computeProgress(
    this.yearData().newClientsGoal.inProgressValue,
    this.yearData().newClientsGoal.targetValue
  ));

  protected readonly topProducts = computed<TopProductViewModel[]>(() => {
    const mode = this.topProductsMode();
    const yearData = this.yearData();
    const products = yearData.topProducts.filter(item => item.salesCount > 0 || item.revenueAmount > 0);

    const totalFromMetric = mode === 'sales'
      ? yearData.metrics.sales.value
      : yearData.metrics.revenue.value;
    const totalFromProducts = mode === 'sales'
      ? products.reduce((sum, item) => sum + item.salesCount, 0)
      : products.reduce((sum, item) => sum + item.revenueAmount, 0);
    const denominator = totalFromMetric > 0 ? totalFromMetric : totalFromProducts;

    const ranked = [...products].sort((a, b) => {
      const left = mode === 'sales' ? a.salesCount : a.revenueAmount;
      const right = mode === 'sales' ? b.salesCount : b.revenueAmount;
      return right - left;
    });

    return ranked.map((item, index) => {
      const value = mode === 'sales' ? item.salesCount : item.revenueAmount;
      const percentage = denominator > 0 ? Math.min(100, (value / denominator) * 100) : 0;
      return {
        id: item.id,
        rank: index + 1,
        name: item.name,
        percentage,
      };
    });
  });

  ngOnInit(): void {
    this.loadDashboardData();
  }

  protected readonly revenueChart = computed(() => {
    const goal = this.yearData().monthlyRevenueGoal;
    const actual = this.yearData().monthlyRevenueActual;

    const width = 760;
    const height = 300;
    const paddingLeft = 16;
    const paddingRight = 16;
    const paddingTop = 20;
    const paddingBottom = 56;

    const plotWidth = width - paddingLeft - paddingRight;
    const plotHeight = height - paddingTop - paddingBottom;

    const maxValue = Math.max(1, ...goal, ...actual);
    const roundedMax = Math.ceil(maxValue / 5000) * 5000;

    const projectX = (index: number): number => {
      if (goal.length <= 1) {
        return paddingLeft;
      }
      return paddingLeft + (plotWidth * index) / (goal.length - 1);
    };

    const projectY = (value: number): number =>
      paddingTop + plotHeight - (value / roundedMax) * plotHeight;

    const toPoints = (values: number[]): string =>
      values.map((value, index) => `${projectX(index)},${projectY(value)}`).join(' ');

    const yTicks = [0, 0.25, 0.5, 0.75, 1].map(ratio => ({
      y: paddingTop + plotHeight * (1 - ratio),
      label: roundedMax * ratio,
    }));

    const xTicks = this.months.map((monthKey, index) => ({
      x: projectX(index),
      monthKey,
    }));

    return {
      width,
      height,
      goalPoints: toPoints(goal),
      actualPoints: toPoints(actual),
      yTicks,
      xTicks,
    };
  });

  protected isComparisonMenuOpen(key: DashboardMetricKey): boolean {
    return this.openComparisonMenu() === key;
  }

  protected toggleComparisonMenu(key: DashboardMetricKey, event: MouseEvent): void {
    event.stopPropagation();
    this.topProductsMenuOpen.set(false);

    this.openComparisonMenu.set(this.openComparisonMenu() === key ? null : key);
  }

  protected selectComparisonPeriod(metricKey: DashboardMetricKey, period: DashboardComparisonPeriod): void {
    this.selectedComparisonByMetric.update(current => ({
      ...current,
      [metricKey]: period,
    }));

    this.openComparisonMenu.set(null);
  }

  protected toggleTopProductsModeMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.openComparisonMenu.set(null);
    this.topProductsMenuOpen.set(!this.topProductsMenuOpen());
  }

  protected selectTopProductsMode(mode: DashboardTopProductsMode): void {
    this.topProductsMode.set(mode);
    this.topProductsMenuOpen.set(false);
  }

  protected openRevenueGoalConfig(): void {
    this.revenueGoalDraft.set([...this.yearData().monthlyRevenueGoal]);
    this.openComparisonMenu.set(null);
    this.topProductsMenuOpen.set(false);
    this.revenueGoalConfigOpen.set(true);
  }

  protected closeRevenueGoalConfig(): void {
    this.revenueGoalConfigOpen.set(false);
  }

  protected saveRevenueGoalConfig(): void {
    this.dashboardService.updateMonthlyRevenueGoal(
      this.currentYear(),
      this.revenueGoalDraft()
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: updated => {
        this.yearData.set(updated);
        this.revenueGoalConfigOpen.set(false);
      },
    });
  }

  protected openSectionGoalConfig(goalKey: DashboardGoalKey): void {
    this.sectionGoalConfigType.set(goalKey);
    this.sectionGoalTargetDraft.set(
      goalKey === 'revenue'
        ? this.yearData().revenueGoal.targetValue
        : this.yearData().newClientsGoal.targetValue
    );
    this.sectionGoalConfigOpen.set(true);
  }

  protected closeSectionGoalConfig(): void {
    this.sectionGoalConfigOpen.set(false);
    this.sectionGoalConfigType.set(null);
  }

  protected saveSectionGoalConfig(): void {
    const goalKey = this.sectionGoalConfigType();
    if (!goalKey) {
      return;
    }

    this.dashboardService.updateGoalTarget(
      this.currentYear(),
      goalKey,
      this.sectionGoalTargetDraft()
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: updated => {
        this.yearData.set(updated);
        this.closeSectionGoalConfig();
      },
    });
  }

  protected updateSectionGoalTargetDraft(rawValue: string): void {
    const value = Number(rawValue);
    this.sectionGoalTargetDraft.set(Number.isFinite(value) && value >= 0 ? Math.round(value) : 0);
  }

  protected updateGoalDraft(index: number, rawValue: string): void {
    const numericValue = Number(rawValue);
    const nextValue = Number.isFinite(numericValue) && numericValue >= 0 ? Math.round(numericValue) : 0;

    this.revenueGoalDraft.update(values => {
      const copy = [...values];
      copy[index] = nextValue;
      return copy;
    });
  }

  protected formatMetricValue(key: DashboardMetricKey, value: number): string {
    switch (key) {
      case 'revenue':
        return this.formatCurrency(value);
      default:
        return this.formatInteger(value);
    }
  }

  protected formatCurrency(value: number): string {
    return new Intl.NumberFormat(this.locale(), {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  protected formatInteger(value: number): string {
    return new Intl.NumberFormat(this.locale(), {
      maximumFractionDigits: 0,
    }).format(value);
  }

  protected formatPercent(value: number): string {
    return `${new Intl.NumberFormat(this.locale(), {
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(Math.abs(value))}%`;
  }

  protected readonly comparisonOptions: DashboardComparisonPeriod[] = ['week', 'month', 'quarter'];

  protected trackByMetric(_: number, metric: MetricCardViewModel): DashboardMetricKey {
    return metric.key;
  }

  protected trackByMonth(index: number): number {
    return index;
  }

  protected trackByTopProduct(_: number, product: TopProductViewModel): string {
    return product.id;
  }

  protected onRevenueModalOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dashboard-modal')) {
      this.closeRevenueGoalConfig();
    }
  }

  protected onSectionGoalModalOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dashboard-modal')) {
      this.closeSectionGoalConfig();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.openComparisonMenu.set(null);
      this.topProductsMenuOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.openComparisonMenu.set(null);
    this.topProductsMenuOpen.set(false);
    if (this.revenueGoalConfigOpen()) {
      this.closeRevenueGoalConfig();
    }
    if (this.sectionGoalConfigOpen()) {
      this.closeSectionGoalConfig();
    }
  }

  private loadDashboardData(): void {
    this.dashboardService.loadDashboard().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: snapshot => {
        this.availableYears.set(snapshot.availableYears.length ? snapshot.availableYears : [snapshot.currentYear]);
        this.currentYear.set(snapshot.currentYear);
        this.yearData.set(this.dashboardService.getYearData(snapshot.currentYear));
      },
      error: () => {
        this.availableYears.set(this.dashboardService.getAvailableYears());
        this.yearData.set(this.dashboardService.getYearData(this.currentYear()));
      },
    });
  }

  private computeProgress(value: number, target: number): number {
    if (target <= 0) {
      return 0;
    }
    return Math.max(0, Math.min(100, (value / target) * 100));
  }

  private locale(): string {
    return 'fr-FR';
  }
}
