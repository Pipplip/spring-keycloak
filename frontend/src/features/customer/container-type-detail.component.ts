import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CatalogueService } from '../../../build/generated/openapi/typescript-angular/api/catalogue.service';
import { ContainerType } from '../../../build/generated/openapi/typescript-angular/model/containerType';
import { problemMessage } from '../../core/api/problem-message';

@Component({
  selector: 'app-container-type-detail',
  standalone: true,
  imports: [MatButtonModule, MatCardModule, RouterLink],
  template: `
    <section aria-labelledby="container-detail-title">
      <a mat-button routerLink="/catalogue">Back to catalogue</a>
      @if (loading()) {
        <p role="status" aria-live="polite">Loading container details…</p>
      }
      @if (error()) {
        <p class="status-message error-message" role="alert">{{ error() }}</p>
      }
      @if (type(); as item) {
        <mat-card appearance="outlined">
          <mat-card-header>
            <mat-card-title id="container-detail-title">{{ item.name }}</mat-card-title>
            <mat-card-subtitle>{{ item.volumeM3 }} m³</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p>{{ item.description }}</p>
            <p>
              <strong>{{ price(item) }}</strong> per container, per day
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a
              mat-flat-button
              [routerLink]="['/book']"
              [queryParams]="{ containerTypeId: item.id }"
            >
              Check availability
            </a>
          </mat-card-actions>
        </mat-card>
      }
    </section>
  `,
})
export class ContainerTypeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalogue = inject(CatalogueService);
  readonly type = signal<ContainerType | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const id = this.route.snapshot.paramMap.get('containerTypeId');
      if (!id) {
        this.error.set('A container type was not selected.');
        return;
      }
      this.type.set(await firstValueFrom(this.catalogue.getContainerType(id)));
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  price(item: ContainerType): string {
    return new Intl.NumberFormat('en-GB', {
      style: 'currency',
      currency: item.currencyCode,
    }).format(item.dailyPriceCents / 100);
  }
}
