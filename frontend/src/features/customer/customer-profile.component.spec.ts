import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CustomerProfilesService } from '../../../build/generated/openapi/typescript-angular/api/customerProfiles.service';
import { CustomerProfile } from '../../../build/generated/openapi/typescript-angular/model/customerProfile';
import { CustomerProfileComponent } from './customer-profile.component';

describe('CustomerProfileComponent', () => {
  const getProfile = vi.fn();
  const createProfile = vi.fn();
  const updateProfile = vi.fn();
  const navigateByUrl = vi.fn();
  const profile: CustomerProfile = {
    email: 'ada@example.test',
    firstName: 'Ada',
    lastName: 'Lovelace',
    companyName: 'Analytical Engines',
    billingAddress: {
      street: 'Main Street',
      houseNumber: '1',
      postalCode: '10115',
      city: 'Berlin',
      countryCode: 'DE',
    },
    createdAt: '2026-11-03T00:00:00Z',
    updatedAt: '2026-11-03T00:00:00Z',
  };

  beforeEach(async () => {
    getProfile.mockReset();
    createProfile.mockReset();
    updateProfile.mockReset();
    navigateByUrl.mockReset();
    await TestBed.configureTestingModule({
      imports: [CustomerProfileComponent],
      providers: [
        {
          provide: CustomerProfilesService,
          useValue: {
            getMyCustomerProfile: getProfile,
            createMyCustomerProfile: createProfile,
            updateMyCustomerProfile: updateProfile,
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ returnUrl: '/bookings' }) } },
        },
        { provide: Router, useValue: { navigateByUrl } },
      ],
    }).compileComponents();
  });

  it('creates a profile from the verified identity without sending identity or ownership fields', async () => {
    getProfile.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    createProfile.mockReturnValue(of(profile));
    const fixture = TestBed.createComponent(CustomerProfileComponent);
    const component = fixture.componentInstance;

    await component.load();
    component.form.setValue({
      companyName: 'Analytical Engines',
      street: 'Main Street',
      houseNumber: '1',
      postalCode: '10115',
      city: 'Berlin',
      countryCode: 'DE',
    });
    await component.save();

    expect(createProfile).toHaveBeenCalledWith({
      companyName: 'Analytical Engines',
      billingAddress: {
        street: 'Main Street',
        houseNumber: '1',
        postalCode: '10115',
        city: 'Berlin',
        countryCode: 'DE',
      },
    });
    expect(component.profile()).toEqual(profile);
    expect(navigateByUrl).toHaveBeenCalledWith('/bookings');
  });

  it('updates only the service-managed profile fields', async () => {
    getProfile.mockReturnValue(of(profile));
    updateProfile.mockReturnValue(of({ ...profile, companyName: 'Updated Company' }));
    const fixture = TestBed.createComponent(CustomerProfileComponent);
    const component = fixture.componentInstance;

    await component.load();
    component.startEditing();
    component.form.controls.companyName.setValue('Updated Company');
    await component.save();

    expect(updateProfile).toHaveBeenCalledWith({
      companyName: 'Updated Company',
      billingAddress: profile.billingAddress,
    });
    expect(component.profile()?.email).toBe(profile.email);
    expect(component.profile()?.firstName).toBe(profile.firstName);
  });
});
