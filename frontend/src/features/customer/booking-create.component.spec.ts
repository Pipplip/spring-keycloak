import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AvailabilityService } from '../../../build/generated/openapi/typescript-angular/api/availability.service';
import { BookingsService } from '../../../build/generated/openapi/typescript-angular/api/bookings.service';
import { CatalogueService } from '../../../build/generated/openapi/typescript-angular/api/catalogue.service';
import { Availability } from '../../../build/generated/openapi/typescript-angular/model/availability';
import { Booking } from '../../../build/generated/openapi/typescript-angular/model/booking';
import { ContainerTypePage } from '../../../build/generated/openapi/typescript-angular/model/containerTypePage';
import { BookingCreateComponent } from './booking-create.component';

describe('BookingCreateComponent', () => {
  const createBooking = vi.fn();
  const catalogue = {
    listContainerTypes: vi.fn().mockReturnValue(
      of({
        items: [],
        page: { page: 0, size: 100, totalElements: 0, totalPages: 0 },
      } satisfies ContainerTypePage),
    ),
  };
  const availabilityApi = {};
  const bookingApi = { createBooking };

  beforeEach(async () => {
    createBooking.mockClear();
    createBooking.mockReturnValue(
      of({
        id: 'booking-1',
        customerId: 'server-owned-customer',
        containerTypeId: 'type-1',
        quantity: 1,
        deliveryDate: '2027-01-10',
        pickupDate: '2027-01-12',
        deliveryAddress: {
          street: 'Main Street',
          houseNumber: '1',
          postalCode: '10115',
          city: 'Berlin',
          countryCode: 'DE',
        },
        status: 'PENDING',
        dailyPriceCents: 5000,
        currencyCode: 'EUR',
        assignedContainerCount: 1,
        createdAt: '2026-11-03T00:00:00Z',
        updatedAt: '2026-11-03T00:00:00Z',
        version: 0,
      } satisfies Booking),
    );
    await TestBed.configureTestingModule({
      imports: [BookingCreateComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
        { provide: CatalogueService, useValue: catalogue },
        { provide: AvailabilityService, useValue: availabilityApi },
        { provide: BookingsService, useValue: bookingApi },
      ],
    }).compileComponents();
  });

  it('does not submit invalid booking input', async () => {
    const fixture = TestBed.createComponent(BookingCreateComponent);
    fixture.detectChanges();

    await fixture.componentInstance.createBooking();

    expect(createBooking).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('Complete all required fields');
  });

  it('clears a previous availability result when booking dates change', () => {
    const fixture = TestBed.createComponent(BookingCreateComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.availability.set({
      containerTypeId: 'type-1',
      deliveryDate: '2027-01-10',
      pickupDate: '2027-01-12',
      quantity: 1,
      available: true,
    } satisfies Availability);

    component.form.controls.deliveryDate.setValue('2027-01-11');

    expect(component.availability()).toBeNull();
  });

  it('submits the generated booking request without a client-selected customer identity', async () => {
    const fixture = TestBed.createComponent(BookingCreateComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.setValue({
      containerTypeId: 'type-1',
      quantity: 1,
      deliveryDate: '2027-01-10',
      pickupDate: '2027-01-12',
      street: 'Main Street',
      houseNumber: '1',
      postalCode: '10115',
      city: 'Berlin',
      countryCode: 'de',
    });
    component.availability.set({
      containerTypeId: 'type-1',
      deliveryDate: '2027-01-10',
      pickupDate: '2027-01-12',
      quantity: 1,
      available: true,
    } satisfies Availability);

    await component.createBooking();

    expect(createBooking).toHaveBeenCalledWith({
      containerTypeId: 'type-1',
      quantity: 1,
      deliveryDate: '2027-01-10',
      pickupDate: '2027-01-12',
      deliveryAddress: {
        street: 'Main Street',
        houseNumber: '1',
        postalCode: '10115',
        city: 'Berlin',
        countryCode: 'DE',
      },
    });
    expect(component.success()).toContain('was created');
  });
});
