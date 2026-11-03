import { Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { BookingsService } from '../../../build/generated/openapi/typescript-angular/api/bookings.service';
import { InvoicesService } from '../../../build/generated/openapi/typescript-angular/api/invoices.service';
import { Booking } from '../../../build/generated/openapi/typescript-angular/model/booking';
import { BookingStatus } from '../../../build/generated/openapi/typescript-angular/model/bookingStatus';
import { Invoice } from '../../../build/generated/openapi/typescript-angular/model/invoice';
import { problemMessage } from '../../core/api/problem-message';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [MatButtonModule, MatCardModule],
  templateUrl: './my-bookings.component.html',
})
export class MyBookingsComponent implements OnInit {
  private readonly bookingsApi = inject(BookingsService);
  private readonly invoicesApi = inject(InvoicesService);

  readonly bookings = signal<Booking[]>([]);
  readonly invoices = signal<Record<string, Invoice>>({});
  readonly loading = signal(true);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly page = signal(0);
  readonly hasMore = signal(false);
  readonly busyBookingId = signal('');

  ngOnInit(): void {
    void this.load();
  }

  async load(page = 0): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const result = await firstValueFrom(this.bookingsApi.listMyBookings(page, 20));
      this.bookings.set(result.items);
      this.page.set(result.page?.page ?? page);
      this.hasMore.set((result.page?.page ?? 0) + 1 < (result.page?.totalPages ?? 0));
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  canCancel(status: BookingStatus): boolean {
    return status === BookingStatus.Pending || status === BookingStatus.Confirmed;
  }

  async cancel(booking: Booking): Promise<void> {
    if (!this.canCancel(booking.status) || !window.confirm('Cancel this booking?')) {
      return;
    }
    this.actionError.set('');
    this.busyBookingId.set(booking.id);
    try {
      const updated = await firstValueFrom(this.bookingsApi.requestBookingCancellation(booking.id));
      this.bookings.update((items) =>
        items.map((item) => (item.id === updated.id ? updated : item)),
      );
    } catch (error: unknown) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busyBookingId.set('');
    }
  }

  async showInvoice(bookingId: string): Promise<void> {
    this.actionError.set('');
    this.busyBookingId.set(bookingId);
    try {
      const invoice = await firstValueFrom(this.invoicesApi.getMyBookingInvoice(bookingId));
      this.invoices.update((current) => ({ ...current, [bookingId]: invoice }));
    } catch (error: unknown) {
      this.actionError.set(problemMessage(error));
    } finally {
      this.busyBookingId.set('');
    }
  }

  money(cents: number, currency: string): string {
    return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(cents / 100);
  }
}
