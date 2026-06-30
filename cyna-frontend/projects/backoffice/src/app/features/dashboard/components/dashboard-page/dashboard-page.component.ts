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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
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
  readonly comparisonDelta: number;
  readonly comparisonPeriodValue: number;
  readonly period: DashboardComparisonPeriod;
}

interface TopProductViewModel {
  readonly id: string;
  readonly rank: number;
  readonly name: string;
  readonly percentage: number;
}

type SalesTrendMode = 'daily' | 'weekly';

interface SalesTrendBar {
  readonly label: string;
  readonly revenue: number;
  readonly salesCount: number;
  readonly x: number;
  readonly y: number;
  readonly width: number;
  readonly height: number;
}

interface SalesTrendChartViewModel {
  readonly width: number;
  readonly height: number;
  readonly bars: SalesTrendBar[];
  readonly baselineY: number;
}

interface CategoryAvgCartBar {
  readonly category: string;
  readonly avgCartValue: number;
  readonly orderCount: number;
  readonly x: number;
  readonly y: number;
  readonly width: number;
  readonly height: number;
}

interface CategoryAvgCartChartViewModel {
  readonly width: number;
  readonly height: number;
  readonly bars: CategoryAvgCartBar[];
  readonly baselineY: number;
}

interface CategoryPieSlice {
  readonly category: string;
  readonly revenue: number;
  readonly quantity: number;
  readonly percentage: number;
  readonly path: string;
  readonly color: string;
}

interface CategoryPieChartViewModel {
  readonly size: number;
  readonly slices: CategoryPieSlice[];
  readonly total: number;
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
  private readonly translate = inject(TranslateService);

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
  protected readonly salesTrendMode = signal<SalesTrendMode>('daily');
  protected readonly categoryAvgCartMode = signal<SalesTrendMode>('daily');
  protected readonly categorySalesMode = signal<SalesTrendMode>('daily');
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
        comparisonDelta: data.revenue.comparisons[selected.revenue],
        comparisonPeriodValue: data.revenue.comparisonValues[selected.revenue],
        period: selected.revenue,
      },
      {
        key: 'clients',
        titleKey: 'dashboard.metrics.clients',
        value: data.clients.value,
        comparisonDelta: data.clients.comparisons[selected.clients],
        comparisonPeriodValue: data.clients.comparisonValues[selected.clients],
        period: selected.clients,
      },
      {
        key: 'sales',
        titleKey: 'dashboard.metrics.sales',
        value: data.sales.value,
        comparisonDelta: data.sales.comparisons[selected.sales],
        comparisonPeriodValue: data.sales.comparisonValues[selected.sales],
        period: selected.sales,
      },
      {
        key: 'activeSubscriptions',
        titleKey: 'dashboard.metrics.activeSubscriptions',
        value: data.activeSubscriptions.value,
        comparisonDelta: data.activeSubscriptions.comparisons[selected.activeSubscriptions],
        comparisonPeriodValue: data.activeSubscriptions.comparisonValues[selected.activeSubscriptions],
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

  // Stable palette reused by both the average-cart histogram and the pie chart
  // so a given category keeps the same color across both charts.
  private static readonly CATEGORY_COLORS = [
    '#6366f1', '#22c55e', '#f59e0b', '#ec4899',
    '#06b6d4', '#a855f7', '#ef4444', '#14b8a6',
  ] as const;

  protected readonly salesTrendChart = computed<SalesTrendChartViewModel>(() => {
    const data = this.yearData();
    const points = this.salesTrendMode() === 'daily' ? data.dailySales : data.weeklySales;

    const width = 760;
    const height = 280;
    const paddingLeft = 16;
    const paddingRight = 16;
    const paddingTop = 20;
    const paddingBottom = 48;

    const plotWidth = width - paddingLeft - paddingRight;
    const plotHeight = height - paddingTop - paddingBottom;
    const baselineY = paddingTop + plotHeight;

    const maxValue = Math.max(1, ...points.map(point => point.revenue));
    const slotWidth = points.length > 0 ? plotWidth / points.length : plotWidth;
    const barWidth = Math.max(4, slotWidth * 0.6);

    const bars = points.map((point, index) => {
      const barHeight = (point.revenue / maxValue) * plotHeight;
      const slotStart = paddingLeft + slotWidth * index;
      return {
        label: point.label,
        revenue: point.revenue,
        salesCount: point.salesCount,
        x: slotStart + (slotWidth - barWidth) / 2,
        y: baselineY - barHeight,
        width: barWidth,
        height: barHeight,
      };
    });

    return { width, height, bars, baselineY };
  });

  protected readonly categoryAvgCartChart = computed<CategoryAvgCartChartViewModel>(() => {
    const data = this.yearData();
    const daily = data.categoryAvgCartDaily;
    const weekly = data.categoryAvgCartWeekly;
    const items = this.categoryAvgCartMode() === 'daily'
      ? (daily.length ? daily : data.categoryAvgCart)
      : (weekly.length ? weekly : data.categoryAvgCart);

    const width = 760;
    const height = 300;
    const paddingLeft = 16;
    const paddingRight = 16;
    const paddingTop = 20;
    const paddingBottom = 56;

    const plotWidth = width - paddingLeft - paddingRight;
    const plotHeight = height - paddingTop - paddingBottom;
    const baselineY = paddingTop + plotHeight;

    const maxValue = Math.max(1, ...items.map(item => item.avgCartValue));
    const slotWidth = items.length > 0 ? plotWidth / items.length : plotWidth;
    const barWidth = Math.max(8, slotWidth * 0.5);

    const bars = items.map((item, index) => {
      const barHeight = (item.avgCartValue / maxValue) * plotHeight;
      const slotStart = paddingLeft + slotWidth * index;
      return {
        category: item.category,
        avgCartValue: item.avgCartValue,
        orderCount: item.orderCount,
        x: slotStart + (slotWidth - barWidth) / 2,
        y: baselineY - barHeight,
        width: barWidth,
        height: barHeight,
      };
    });

    return { width, height, bars, baselineY };
  });

  protected readonly categoryPieChart = computed<CategoryPieChartViewModel>(() => {
    const data = this.yearData();
    const daily = data.categorySalesDaily;
    const weekly = data.categorySalesWeekly;
    const items = this.categorySalesMode() === 'daily'
      ? (daily.length ? daily : data.categorySales)
      : (weekly.length ? weekly : data.categorySales);
    const size = 240;
    const radius = size / 2;
    const center = size / 2;

    const total = items.reduce((sum, item) => sum + item.revenue, 0);
    if (total <= 0) {
      return { size, slices: [], total: 0 };
    }

    let startAngle = -Math.PI / 2;
    const slices = items.map((item, index) => {
      const fraction = item.revenue / total;
      const endAngle = startAngle + fraction * 2 * Math.PI;
      const path = this.describeArc(center, center, radius, startAngle, endAngle, fraction);
      startAngle = endAngle;
      return {
        category: item.category,
        revenue: item.revenue,
        quantity: item.quantity,
        percentage: fraction * 100,
        path,
        color: this.categoryColor(index),
      };
    });

    return { size, slices, total };
  });

  protected categoryColor(index: number): string {
    const colors = DashboardPageComponent.CATEGORY_COLORS;
    return colors[index % colors.length];
  }

  protected toggleSalesTrendMode(mode: SalesTrendMode): void {
    this.salesTrendMode.set(mode);
  }

  protected toggleCategoryAvgCartMode(mode: SalesTrendMode): void {
    this.categoryAvgCartMode.set(mode);
  }

  protected toggleCategorySalesMode(mode: SalesTrendMode): void {
    this.categorySalesMode.set(mode);
  }

  // Builds an SVG pie slice path. A single full-circle slice is drawn as two
  // arcs so the 360° large-arc case still renders.
  private describeArc(
    cx: number,
    cy: number,
    radius: number,
    startAngle: number,
    endAngle: number,
    fraction: number,
  ): string {
    if (fraction >= 0.9999) {
      const midAngle = startAngle + Math.PI;
      const mid = this.polarToCartesian(cx, cy, radius, midAngle);
      const start = this.polarToCartesian(cx, cy, radius, startAngle);
      return [
        `M ${cx} ${cy}`,
        `L ${start.x} ${start.y}`,
        `A ${radius} ${radius} 0 1 1 ${mid.x} ${mid.y}`,
        `A ${radius} ${radius} 0 1 1 ${start.x} ${start.y}`,
        'Z',
      ].join(' ');
    }

    const start = this.polarToCartesian(cx, cy, radius, startAngle);
    const end = this.polarToCartesian(cx, cy, radius, endAngle);
    const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;
    return [
      `M ${cx} ${cy}`,
      `L ${start.x} ${start.y}`,
      `A ${radius} ${radius} 0 ${largeArcFlag} 1 ${end.x} ${end.y}`,
      'Z',
    ].join(' ');
  }

  private polarToCartesian(cx: number, cy: number, radius: number, angle: number): { x: number; y: number } {
    return {
      x: cx + radius * Math.cos(angle),
      y: cy + radius * Math.sin(angle),
    };
  }

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

  protected formatTrendLabel(label: string): string {
    if (!label) {
      return '';
    }
    const date = new Date(`${label}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
      return label;
    }
    if (this.salesTrendMode() === 'weekly') {
      return new Intl.DateTimeFormat(this.locale(), { day: '2-digit', month: '2-digit' }).format(date);
    }
    return new Intl.DateTimeFormat(this.locale(), { weekday: 'short', day: '2-digit' }).format(date);
  }

  protected trackByIndex(index: number): number {
    return index;
  }

  protected formatPercent(value: number): string {
    return `${new Intl.NumberFormat(this.locale(), {
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(Math.abs(value))}%`;
  }

  private static readonly STATUS_ORDER = ['PAID', 'FULFILLED', 'PENDING', 'CONFIRMED', 'CANCELLED'] as const;

  protected readonly orderStatusTotal = computed(() =>
    Object.values(this.yearData().ordersByStatus).reduce((sum, v) => sum + v, 0)
  );

  protected readonly orderStatusItems = computed(() => {
    const statuses = this.yearData().ordersByStatus;
    const total = this.orderStatusTotal();
    return DashboardPageComponent.STATUS_ORDER
      .filter(s => s in statuses)
      .map(s => ({
        status: s,
        count: statuses[s] ?? 0,
        percentage: total > 0 ? Math.min(100, ((statuses[s] ?? 0) / total) * 100) : 0,
      }));
  });

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
    return this.translate.getCurrentLang() === 'en' ? 'en-US' : 'fr-FR';
  }
}
