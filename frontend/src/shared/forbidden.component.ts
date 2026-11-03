import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [MatButtonModule, RouterLink],
  template: `
    <section aria-labelledby="forbidden-title">
      <h1 class="page-heading" id="forbidden-title">Access unavailable</h1>
      <p class="page-intro">Your account does not have permission to view this page.</p>
      <a mat-flat-button routerLink="/catalogue">Return to catalogue</a>
    </section>
  `,
})
export class ForbiddenComponent {}
