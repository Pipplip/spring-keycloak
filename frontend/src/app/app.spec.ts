import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from '../core/auth/auth.service';

describe('App', () => {
  let fixture: ComponentFixture<App>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => false,
            hasRole: () => false,
            initializationError: () => '',
            login: vi.fn(),
            logout: vi.fn(),
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  });

  it('renders the shared navigation and a keyboard skip link', () => {
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('a.skip-link')?.getAttribute('href')).toBe('#main-content');
    expect(root.querySelector('nav[aria-label="Primary"]')).not.toBeNull();
    expect(root.querySelector('main#main-content')).not.toBeNull();
    expect(root.textContent).toContain('Register');
    expect(root.textContent).toContain('Sign in');
  });
});
