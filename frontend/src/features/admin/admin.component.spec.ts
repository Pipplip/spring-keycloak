import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdministrationService } from '../../../build/generated/openapi/typescript-angular/api/administration.service';
import { InvoicesService } from '../../../build/generated/openapi/typescript-angular/api/invoices.service';
import { ContainerType } from '../../../build/generated/openapi/typescript-angular/model/containerType';
import { ContainerTypePage } from '../../../build/generated/openapi/typescript-angular/model/containerTypePage';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  const createType = vi.fn();
  const adminApi = {
    listAdminBookings: vi
      .fn()
      .mockReturnValue(
        of({ items: [], page: { page: 0, size: 100, totalElements: 0, totalPages: 0 } }),
      ),
    listAdminContainerTypes: vi.fn().mockReturnValue(
      of({
        items: [],
        page: { page: 0, size: 100, totalElements: 0, totalPages: 0 },
      } satisfies ContainerTypePage),
    ),
    createAdminContainerType: createType,
  };

  beforeEach(async () => {
    createType.mockClear();
    createType.mockReturnValue(
      of({
        id: 'type-1',
        name: 'Small skip',
        description: 'Compact waste container',
        volumeM3: 5,
        dailyPriceCents: 5000,
        currencyCode: 'EUR',
        active: true,
      } satisfies ContainerType),
    );
    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        { provide: AdministrationService, useValue: adminApi },
        { provide: InvoicesService, useValue: {} },
      ],
    }).compileComponents();
  });

  it('blocks invalid forms before calling an administration API', async () => {
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.saveType();

    expect(createType).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('Complete the required');
  });

  it('creates a container type through the generated client', async () => {
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.typeForm.setValue({
      name: 'Small skip',
      description: 'Compact waste container',
      volumeM3: 5,
      dailyPriceCents: 5000,
      currencyCode: 'eur',
      active: true,
    });

    await fixture.componentInstance.saveType();

    expect(createType).toHaveBeenCalledWith({
      name: 'Small skip',
      description: 'Compact waste container',
      volumeM3: 5,
      dailyPriceCents: 5000,
      currencyCode: 'EUR',
      active: true,
    });
    expect(fixture.componentInstance.success()).toBe('Container type created.');
  });
});
