import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { AdministrationService } from '../../../build/generated/openapi/typescript-angular/api/administration.service';
import { InvoicesService } from '../../../build/generated/openapi/typescript-angular/api/invoices.service';
import { Booking } from '../../../build/generated/openapi/typescript-angular/model/booking';
import { BookingStatus } from '../../../build/generated/openapi/typescript-angular/model/bookingStatus';
import { Container } from '../../../build/generated/openapi/typescript-angular/model/container';
import { ContainerStatus } from '../../../build/generated/openapi/typescript-angular/model/containerStatus';
import { ContainerType } from '../../../build/generated/openapi/typescript-angular/model/containerType';
import { ContainerTypeRequest } from '../../../build/generated/openapi/typescript-angular/model/containerTypeRequest';
import { ContainerAssignment } from '../../../build/generated/openapi/typescript-angular/model/containerAssignment';
import { Customer } from '../../../build/generated/openapi/typescript-angular/model/customer';
import { Invoice } from '../../../build/generated/openapi/typescript-angular/model/invoice';
import { InvoiceStatus } from '../../../build/generated/openapi/typescript-angular/model/invoiceStatus';
import { problemMessage } from '../../core/api/problem-message';

type AdminSection = 'bookings' | 'types' | 'containers' | 'customers' | 'invoices';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(AdministrationService);
  private readonly invoicesApi = inject(InvoicesService);

  readonly section = signal<AdminSection>('bookings');
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly bookings = signal<Booking[]>([]);
  readonly types = signal<ContainerType[]>([]);
  readonly containers = signal<Container[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly invoices = signal<Invoice[]>([]);
  readonly bookingDetail = signal<Booking | null>(null);
  readonly assignments = signal<ContainerAssignment[]>([]);
  readonly assignmentsBookingId = signal('');
  readonly selectedTypeId = signal('');
  readonly selectedContainerId = signal('');
  readonly selectedCustomerId = signal('');

  readonly bookingStatuses = Object.values(BookingStatus);
  readonly containerStatuses = Object.values(ContainerStatus);

  readonly typeForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    volumeM3: [1, [Validators.required, Validators.min(0.01)]],
    dailyPriceCents: [0, [Validators.required, Validators.min(0)]],
    currencyCode: ['EUR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    active: this.fb.control<boolean>(true),
  });
  readonly containerForm = this.fb.group({
    inventoryNumber: ['', Validators.required],
    containerTypeId: ['', Validators.required],
    status: this.fb.control<ContainerStatus>(ContainerStatus.Available, Validators.required),
  });
  readonly customerForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    companyName: [''],
    street: ['', Validators.required],
    houseNumber: ['', Validators.required],
    postalCode: ['', Validators.required],
    city: ['', Validators.required],
    countryCode: ['DE', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
  });
  readonly assignmentForm = this.fb.group({
    bookingId: ['', Validators.required],
    containerId: ['', Validators.required],
  });
  readonly invoiceBookingForm = this.fb.group({
    bookingId: ['', Validators.required],
  });

  ngOnInit(): void {
    void this.load();
  }

  async viewBooking(bookingId: string): Promise<void> {
    await this.perform(async () => {
      this.bookingDetail.set(await firstValueFrom(this.api.getAdminBooking(bookingId)));
    });
  }

  async selectSection(index: number): Promise<void> {
    const sections: AdminSection[] = ['bookings', 'types', 'containers', 'customers', 'invoices'];
    const selected = sections[index];
    if (selected) {
      this.section.set(selected);
      await this.load();
    }
  }

  async load(): Promise<void> {
    this.error.set('');
    this.loading.set(true);
    try {
      switch (this.section()) {
        case 'bookings':
          this.bookings.set((await firstValueFrom(this.api.listAdminBookings(0, 100))).items);
          break;
        case 'types':
          this.types.set((await firstValueFrom(this.api.listAdminContainerTypes(0, 100))).items);
          break;
        case 'containers':
          this.containers.set((await firstValueFrom(this.api.listAdminContainers(0, 100))).items);
          this.types.set((await firstValueFrom(this.api.listAdminContainerTypes(0, 100))).items);
          break;
        case 'customers':
          this.customers.set((await firstValueFrom(this.api.listAdminCustomers(0, 100))).items);
          break;
        case 'invoices':
          this.invoices.set(
            (await firstValueFrom(this.invoicesApi.listAdminInvoices(0, 100))).items,
          );
          break;
      }
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async saveType(): Promise<void> {
    if (this.typeForm.invalid) {
      this.typeForm.markAllAsTouched();
      this.error.set('Complete the required container type fields.');
      return;
    }
    const value = this.typeForm.getRawValue();
    const request: ContainerTypeRequest = {
      ...value,
      currencyCode: value.currencyCode.toUpperCase(),
    };
    await this.perform(async () => {
      if (this.selectedTypeId()) {
        await firstValueFrom(this.api.updateAdminContainerType(this.selectedTypeId(), request));
        this.success.set('Container type updated.');
      } else {
        await firstValueFrom(this.api.createAdminContainerType(request));
        this.success.set('Container type created.');
      }
      this.selectedTypeId.set('');
      this.typeForm.reset({
        name: '',
        description: '',
        volumeM3: 1,
        dailyPriceCents: 0,
        currencyCode: 'EUR',
        active: true,
      });
      this.types.set((await firstValueFrom(this.api.listAdminContainerTypes(0, 100))).items);
    });
  }

  editType(type: ContainerType): void {
    this.selectedTypeId.set(type.id);
    this.typeForm.setValue({
      name: type.name,
      description: type.description,
      volumeM3: type.volumeM3,
      dailyPriceCents: type.dailyPriceCents,
      currencyCode: type.currencyCode,
      active: type.active,
    });
  }

  async saveContainer(): Promise<void> {
    if (this.containerForm.invalid) {
      this.containerForm.markAllAsTouched();
      this.error.set('Complete the required container fields.');
      return;
    }
    const request = this.containerForm.getRawValue();
    await this.perform(async () => {
      if (this.selectedContainerId()) {
        await firstValueFrom(this.api.updateAdminContainer(this.selectedContainerId(), request));
        this.success.set('Container updated.');
      } else {
        await firstValueFrom(this.api.createAdminContainer(request));
        this.success.set('Container created.');
      }
      this.selectedContainerId.set('');
      this.containerForm.reset({
        inventoryNumber: '',
        containerTypeId: '',
        status: ContainerStatus.Available,
      });
      this.containers.set((await firstValueFrom(this.api.listAdminContainers(0, 100))).items);
    });
  }

  editContainer(container: Container): void {
    this.selectedContainerId.set(container.id);
    this.containerForm.setValue({
      inventoryNumber: container.inventoryNumber,
      containerTypeId: container.containerTypeId,
      status: container.status,
    });
  }

  async updateCustomer(): Promise<void> {
    if (!this.selectedCustomerId() || this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      this.error.set(
        this.selectedCustomerId()
          ? 'Complete the required customer fields.'
          : 'Select a customer to edit.',
      );
      return;
    }
    const value = this.customerForm.getRawValue();
    await this.perform(async () => {
      await firstValueFrom(
        this.api.updateAdminCustomer(this.selectedCustomerId(), {
          email: value.email,
          firstName: value.firstName,
          lastName: value.lastName,
          companyName: value.companyName || undefined,
          billingAddress: {
            street: value.street,
            houseNumber: value.houseNumber,
            postalCode: value.postalCode,
            city: value.city,
            countryCode: value.countryCode.toUpperCase(),
          },
        }),
      );
      this.success.set('Customer profile updated.');
      this.selectedCustomerId.set('');
      this.customers.set((await firstValueFrom(this.api.listAdminCustomers(0, 100))).items);
    });
  }

  editCustomer(customer: Customer): void {
    this.selectedCustomerId.set(customer.id);
    this.customerForm.setValue({
      email: customer.email,
      firstName: customer.firstName,
      lastName: customer.lastName,
      companyName: customer.companyName ?? '',
      street: customer.billingAddress.street,
      houseNumber: customer.billingAddress.houseNumber,
      postalCode: customer.billingAddress.postalCode,
      city: customer.billingAddress.city,
      countryCode: customer.billingAddress.countryCode,
    });
  }

  async changeBookingStatus(booking: Booking, status: BookingStatus): Promise<void> {
    if (
      status === booking.status ||
      !window.confirm(`Change booking status from ${booking.status} to ${status}?`)
    ) {
      return;
    }
    await this.perform(async () => {
      const updated = await firstValueFrom(
        this.api.updateAdminBookingStatus(booking.id, { status }),
      );
      this.bookings.update((items) =>
        items.map((item) => (item.id === updated.id ? updated : item)),
      );
      this.success.set(`Booking status changed to ${updated.status}.`);
    });
  }

  async loadAssignments(): Promise<void> {
    const bookingId = this.assignmentForm.controls.bookingId.value.trim();
    if (!bookingId) {
      this.assignmentForm.controls.bookingId.markAsTouched();
      this.error.set('Enter a booking ID to view its assignments.');
      return;
    }
    await this.perform(() => this.fetchAssignments(bookingId));
  }

  async assignContainer(): Promise<void> {
    if (this.assignmentForm.invalid) {
      this.assignmentForm.markAllAsTouched();
      this.error.set('Enter both a booking ID and a container ID.');
      return;
    }
    const { bookingId, containerId } = this.assignmentForm.getRawValue();
    await this.perform(async () => {
      await firstValueFrom(this.api.assignAdminContainerToBooking(bookingId, { containerId }));
      await this.fetchAssignments(bookingId);
      this.bookings.set((await firstValueFrom(this.api.listAdminBookings(0, 100))).items);
      this.success.set('Container assigned to booking.');
    });
  }

  async releaseAssignment(assignmentId: string): Promise<void> {
    const bookingId = this.assignmentsBookingId();
    if (!bookingId || !window.confirm('Release this container assignment?')) {
      return;
    }
    await this.perform(async () => {
      await firstValueFrom(this.api.releaseAdminContainerAssignment(bookingId, assignmentId));
      await this.fetchAssignments(bookingId);
      this.success.set('Container assignment released.');
    });
  }

  async createInvoice(): Promise<void> {
    if (this.invoiceBookingForm.invalid) {
      this.invoiceBookingForm.markAllAsTouched();
      this.error.set('Enter a booking ID.');
      return;
    }
    const bookingId = this.invoiceBookingForm.controls.bookingId.value.trim();
    await this.perform(async () => {
      await firstValueFrom(this.invoicesApi.createAdminInvoiceForBooking(bookingId));
      this.success.set('Invoice created.');
      this.invoices.set((await firstValueFrom(this.invoicesApi.listAdminInvoices(0, 100))).items);
    });
  }

  async invoiceAction(invoice: Invoice, action: 'send' | 'paid' | 'void'): Promise<void> {
    const description = action === 'paid' ? 'mark this invoice as paid' : `${action} this invoice`;
    if (!window.confirm(`Are you sure you want to ${description}?`)) {
      return;
    }
    await this.perform(async () => {
      let updated: Invoice;
      switch (action) {
        case 'send':
          updated = await firstValueFrom(this.invoicesApi.sendAdminInvoice(invoice.id));
          break;
        case 'paid':
          updated = await firstValueFrom(this.invoicesApi.markAdminInvoicePaid(invoice.id));
          break;
        case 'void':
          updated = await firstValueFrom(this.invoicesApi.voidAdminInvoice(invoice.id));
          break;
      }
      this.invoices.update((items) =>
        items.map((item) => (item.id === updated.id ? updated : item)),
      );
      this.success.set(`Invoice ${updated.invoiceNumber} is now ${updated.status}.`);
    });
  }

  money(cents: number, currency: string): string {
    return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(cents / 100);
  }

  private async perform(action: () => Promise<void>): Promise<void> {
    this.error.set('');
    this.success.set('');
    this.loading.set(true);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async fetchAssignments(bookingId: string): Promise<void> {
    const page = await firstValueFrom(this.api.listAdminContainerAssignments(bookingId, 0, 100));
    this.assignments.set(page.items);
    this.assignmentsBookingId.set(bookingId);
  }
}
