import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardService, DashboardYearData } from './dashboard.service';
import { environment } from '../../../../environments/environment';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DashboardService,
      ],
    });

    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load dashboard snapshot and cache year data', () => {
    let snapshot: any;
    service.loadDashboard().subscribe(result => {
      snapshot = result;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/dashboard`);
    expect(req.request.method).toBe('GET');

    req.flush({
      success: true,
      data: {
        currentYear: 2026,
        availableYears: [2025, 2026],
        years: [
          buildYearData(2025, 812000, 4520, 10820, 4290, 980000, 5100),
          buildYearData(2026, 81000, 5000, 12000, 5000, 500000, 3200),
        ],
      },
      timestamp: '2026-05-23T08:00:00Z',
    });

    expect(snapshot.currentYear).toBe(2026);
    expect(snapshot.availableYears).toEqual([2025, 2026]);
    expect(snapshot.years.length).toBe(2);
    expect(service.currentYear).toBe(2026);
    expect(service.getAvailableYears()).toEqual([2025, 2026]);
    expect(service.getYearData(2026).metrics.revenue.value).toBe(81000);
  });

  it('should update annual target and refresh cache', () => {
    primeDashboardCache();

    let updatedYear: DashboardYearData | undefined;
    service.updateGoalTarget(2026, 'clients', 4000).subscribe(result => {
      updatedYear = result;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/dashboard/2026/goals/target`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      goalKey: 'clients',
      targetValue: 4000,
    });

    const updated = buildYearData(2026, 81000, 5000, 12000, 5000, 500000, 4000);
    req.flush({
      success: true,
      data: updated,
      timestamp: '2026-05-23T09:00:00Z',
    });

    expect(updatedYear?.newClientsGoal.targetValue).toBe(4000);
    expect(service.getYearData(2026).newClientsGoal.targetValue).toBe(4000);
  });

  it('should update monthly revenue goals and sync annual target from API', () => {
    primeDashboardCache();

    const monthlyGoal = [1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 2000];
    let updatedYear: DashboardYearData | undefined;

    service.updateMonthlyRevenueGoal(2026, monthlyGoal).subscribe(result => {
      updatedYear = result;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/dashboard/2026/goals/monthly-revenue`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.monthlyRevenueGoal).toEqual(monthlyGoal);

    const updated = {
      ...buildYearData(2026, 81000, 5000, 12000, 5000, 13000, 3200),
      monthlyRevenueGoal: [...monthlyGoal],
    };

    req.flush({
      success: true,
      data: updated,
      timestamp: '2026-05-23T09:20:00Z',
    });

    expect(updatedYear?.revenueGoal.targetValue).toBe(13000);
    expect(updatedYear?.monthlyRevenueGoal).toEqual(monthlyGoal);
    expect(service.getYearData(2026).monthlyRevenueGoal).toEqual(monthlyGoal);
  });

  function primeDashboardCache(): void {
    service.loadDashboard().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/admin/dashboard`);
    req.flush({
      success: true,
      data: {
        currentYear: 2026,
        availableYears: [2026],
        years: [buildYearData(2026, 81000, 5000, 12000, 5000, 500000, 3200)],
      },
      timestamp: '2026-05-23T08:00:00Z',
    });
  }

  function buildYearData(
    year: number,
    revenueValue: number,
    clientsValue: number,
    salesValue: number,
    subscriptionsValue: number,
    revenueTarget: number,
    clientsTarget: number
  ): DashboardYearData {
    return {
      year,
      metrics: {
        revenue: { value: revenueValue, comparisons: { week: 10.6, month: 7.4, quarter: 12.2 } },
        clients: { value: clientsValue, comparisons: { week: 1.5, month: 2.8, quarter: 4.9 } },
        sales: { value: salesValue, comparisons: { week: 3.6, month: 4.3, quarter: 7.1 } },
        activeSubscriptions: { value: subscriptionsValue, comparisons: { week: -1.5, month: -0.9, quarter: 1.1 } },
      },
      revenueGoal: { inProgressValue: 231032, targetValue: revenueTarget },
      newClientsGoal: { inProgressValue: 1880, targetValue: clientsTarget },
      monthlyRevenueGoal: [28000, 30000, 32000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000],
      monthlyRevenueActual: [21000, 22500, 24000, 27500, 29800, 32100, 33900, 36200, 38800, 41400, 44200, 47600],
      topProducts: [
        { id: 'soc', name: 'SOC CYNA', salesCount: 1380, revenueAmount: 94000 },
        { id: 'edr', name: 'EDR CYBA', salesCount: 980, revenueAmount: 61200 },
      ],
    };
  }
});
