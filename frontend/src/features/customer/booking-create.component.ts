import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom, merge } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AvailabilityService } from '../../../build/generated/openapi/typescript-angular/api/availability.service';
import { BookingsService } from '../../../build/generated/openapi/typescript-angular/api/bookings.service';
import { CatalogueService } from '../../../build/generated/openapi/typescript-angular/api/catalogue.service';
import { Availability } from '../../../build/generated/openapi/typescript-angular/model/availability';
import { ContainerType } from '../../../build/generated/openapi/typescript-angular/model/containerType';
import { problemMessage } from '../../core/api/problem-message';

@Component({
  selector: 'app-booking-create',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './booking-create.component.html',
})
export class BookingCreateComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly catalogue = inject(CatalogueService);
  private readonly availabilityApi = inject(AvailabilityService);
  private readonly bookingsApi = inject(BookingsService);

  readonly types = signal<ContainerType[]>([]);
  readonly loading = signal(false);
  readonly checking = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly availability = signal<Availability | null>(null);

  readonly form = this.fb.group({
    containerTypeId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
    deliveryDate: ['', Validators.required],
    pickupDate: ['', Validators.required],
    street: ['', Validators.required],
    houseNumber: ['', Validators.required],
    postalCode: ['', Validators.required],
    city: ['', Validators.required],
    countryCode: ['DE', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
  });

  ngOnInit(): void {
    merge(
      this.form.controls.containerTypeId.valueChanges,
      this.form.controls.quantity.valueChanges,
      this.form.controls.deliveryDate.valueChanges,
      this.form.controls.pickupDate.valueChanges,
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.availability.set(null));
    const typeId = this.route.snapshot.queryParamMap.get('containerTypeId');
    if (typeId) {
      this.form.controls.containerTypeId.setValue(typeId);
    }
    void this.loadTypes();
  }

  async loadTypes(): Promise<void> {
    this.loading.set(true);
    try {
      const page = await firstValueFrom(this.catalogue.listContainerTypes(0, 100));
      this.types.set(page.items);
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async checkAvailability(): Promise<void> {
    this.error.set('');
    this.success.set('');
    this.availability.set(null);
    const form = this.form.getRawValue();
    if (
      !this.form.controls.containerTypeId.valid ||
      !this.form.controls.quantity.valid ||
      !form.deliveryDate ||
      !form.pickupDate ||
      form.pickupDate <= form.deliveryDate
    ) {
      this.form.controls.containerTypeId.markAsTouched();
      this.form.controls.quantity.markAsTouched();
      this.form.controls.deliveryDate.markAsTouched();
      this.form.controls.pickupDate.markAsTouched();
      this.error.set('Choose a container type and quantity, and a pickup date after delivery.');
      return;
    }

    this.checking.set(true);
    try {
      this.availability.set(
        await firstValueFrom(
          this.availabilityApi.checkAvailability(
            form.containerTypeId,
            form.deliveryDate,
            form.pickupDate,
            form.quantity,
          ),
        ),
      );
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.checking.set(false);
    }
  }

  async createBooking(): Promise<void> {
    this.error.set('');
    this.success.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Complete all required fields before creating a booking.');
      return;
    }
    const values = this.form.getRawValue();
    if (!values.deliveryDate || !values.pickupDate || values.pickupDate <= values.deliveryDate) {
      this.error.set('Choose a pickup date after delivery.');
      return;
    }
    if (!this.availability()?.available) {
      this.error.set(
        'Check availability and make sure the requested quantity is available before booking.',
      );
      return;
    }

    this.loading.set(true);
    try {
      const booking = await firstValueFrom(
        this.bookingsApi.createBooking({
          containerTypeId: values.containerTypeId,
          quantity: values.quantity,
          deliveryDate: values.deliveryDate,
          pickupDate: values.pickupDate,
          deliveryAddress: {
            street: values.street,
            houseNumber: values.houseNumber,
            postalCode: values.postalCode,
            city: values.city,
            countryCode: values.countryCode.toUpperCase(),
          },
        }),
      );
      this.success.set(`Booking ${booking.id} was created with status ${booking.status}.`);
      this.availability.set(null);
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
