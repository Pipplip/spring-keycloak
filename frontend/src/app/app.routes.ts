import { Routes } from '@angular/router';
import { authenticatedGuard, customerProfileGuard, roleGuard } from '../core/auth/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalogue' },
  {
    path: 'catalogue/:containerTypeId',
    loadComponent: () =>
      import('../features/customer/container-type-detail.component').then(
        (module) => module.ContainerTypeDetailComponent,
      ),
  },
  {
    path: 'catalogue',
    loadComponent: () =>
      import('../features/customer/catalogue.component').then(
        (module) => module.CatalogueComponent,
      ),
  },
  {
    path: 'profile',
    canActivate: [authenticatedGuard, roleGuard('USER')],
    loadComponent: () =>
      import('../features/customer/customer-profile.component').then(
        (module) => module.CustomerProfileComponent,
      ),
  },
  {
    path: 'book',
    canActivate: [authenticatedGuard, roleGuard('USER'), customerProfileGuard],
    loadComponent: () =>
      import('../features/customer/booking-create.component').then(
        (module) => module.BookingCreateComponent,
      ),
  },
  {
    path: 'bookings',
    canActivate: [authenticatedGuard, roleGuard('USER'), customerProfileGuard],
    loadComponent: () =>
      import('../features/customer/my-bookings.component').then(
        (module) => module.MyBookingsComponent,
      ),
  },
  {
    path: 'admin',
    canActivate: [authenticatedGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('../features/admin/admin.component').then((module) => module.AdminComponent),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('../shared/forbidden.component').then((module) => module.ForbiddenComponent),
  },
  { path: '**', redirectTo: 'catalogue' },
];
