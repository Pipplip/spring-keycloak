import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CatalogueService } from '../../../build/generated/openapi/typescript-angular/api/catalogue.service';
import { ContainerType } from '../../../build/generated/openapi/typescript-angular/model/containerType';
import { problemMessage } from '../../core/api/problem-message';

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [MatButtonModule, MatCardModule, RouterLink],
  templateUrl: './catalogue.component.html',
})
export class CatalogueComponent implements OnInit {
  private readonly catalogue = inject(CatalogueService);
  readonly types = signal<ContainerType[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly page = signal(0);
  readonly hasMore = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  async load(page = 0): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const result = await firstValueFrom(this.catalogue.listContainerTypes(page, 12));
      this.types.update((current) => (page === 0 ? result.items : [...current, ...result.items]));
      this.page.set(result?.page?.page ?? page);
      this.hasMore.set((result?.page?.page ?? 0) + 1 < (result?.page?.totalPages ?? 0));
    } catch (error: unknown) {
      this.error.set(problemMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  price(type: ContainerType): string {
    return new Intl.NumberFormat('en-GB', {
      style: 'currency',
      currency: type.currencyCode,
    }).format(type.dailyPriceCents / 100);
  }
}
