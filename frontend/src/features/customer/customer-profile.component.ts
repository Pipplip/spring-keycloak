import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CustomerProfilesService } from '../../../build/generated/openapi/typescript-angular/api/customerProfiles.service';
import { CustomerProfile } from '../../../build/generated/openapi/typescript-angular/model/customerProfile';
import { CustomerProfileRequest } from '../../../build/generated/openapi/typescript-angular/model/customerProfileRequest';
import { problemMessage } from '../../core/api/problem-message';

@Component({
  selector: 'app-customer-profile',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  templateUrl: './customer-profile.component.html',
})
export class CustomerProfileComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly profilesApi = inject(CustomerProfilesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly profile = signal<CustomerProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly creating = signal(false);
  readonly editing = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly form = this.fb.group({
    companyName: ['', Validators.maxLength(255)],
    street: ['', [Validators.required, Validators.maxLength(255)]],
    houseNumber: ['', [Validators.required, Validators.maxLength(50)]],
    postalCode: ['', [Validators.required, Validators.maxLength(32)]],
    city: ['', [Validators.required, Validators.maxLength(255)]],
    countryCode: ['DE', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
  });

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const profile = await firstValueFrom(this.profilesApi.getMyCustomerProfile());
      this.profile.set(profile);
      this.creating.set(false);
      this.editing.set(false);
      this.populateForm(profile);
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this.profile.set(null);
        this.creating.set(true);
        this.editing.set(true);
        this.populateForm();
      } else {
        this.error.set(problemMessage(error));
      }
    } finally {
      this.loading.set(false);
    }
  }

  startEditing(): void {
    const profile = this.profile();
    if (profile) {
      this.populateForm(profile);
      this.editing.set(true);
      this.error.set('');
      this.success.set('');
    }
  }

  cancelEditing(): void {
    const profile = this.profile();
    if (profile) {
      this.populateForm(profile);
      this.editing.set(false);
      this.error.set('');
    }
  }

  async save(): Promise<void> {
    this.error.set('');
    this.success.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Complete the required profile fields before saving.');
      return;
    }

    const wasCreating = this.creating();
    const values = this.form.getRawValue();
    const request: CustomerProfileRequest = {
      companyName: values.companyName || undefined,
      billingAddress: {
        street: values.street,
        houseNumber: values.houseNumber,
        postalCode: values.postalCode,
        city: values.city,
        countryCode: values.countryCode,
      },
    };

    this.saving.set(true);
    try {
      const profile = await firstValueFrom(
        wasCreating
          ? this.profilesApi.createMyCustomerProfile(request)
          : this.profilesApi.updateMyCustomerProfile(request),
      );
      this.profile.set(profile);
      this.creating.set(false);
      this.editing.set(false);
      this.populateForm(profile);
      this.success.set(
        wasCreating ? 'Your customer profile has been created.' : 'Your profile has been updated.',
      );
      if (wasCreating) {
        await this.router.navigateByUrl(this.profileReturnUrl());
      }
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  private populateForm(profile?: CustomerProfile): void {
    this.form.setValue({
      companyName: profile?.companyName ?? '',
      street: profile?.billingAddress.street ?? '',
      houseNumber: profile?.billingAddress.houseNumber ?? '',
      postalCode: profile?.billingAddress.postalCode ?? '',
      city: profile?.billingAddress.city ?? '',
      countryCode: profile?.billingAddress.countryCode ?? 'DE',
    });
  }

  private profileReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    return returnUrl?.startsWith('/book') && !returnUrl.startsWith('//') ? returnUrl : '/bookings';
  }
}
