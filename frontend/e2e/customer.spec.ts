import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const username = process.env.E2E_USER_USERNAME;
const password = process.env.E2E_USER_PASSWORD;
const registrationPassword = process.env.E2E_REGISTRATION_PASSWORD;

async function signInAsCustomer(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/catalogue');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByLabel(/username/i).fill(username ?? '');
  await page.getByLabel(/password/i).fill(password ?? '');
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL('http://localhost:4200/**');
  await completeProfileIfNeeded(page);
}

async function completeProfileIfNeeded(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/bookings');
  await expect(
    page.getByRole('heading', { name: /My bookings|Complete your customer profile/ }),
  ).toBeVisible();
  if (new URL(page.url()).pathname !== '/profile') {
    return;
  }

  await page.getByLabel('Street').fill('Test Street');
  await page.getByLabel('House number').fill('1');
  await page.getByLabel('Postal code').fill('10115');
  await page.getByLabel('City').fill('Berlin');
  await page.getByLabel('Country code').fill('DE');
  await page.getByRole('button', { name: 'Create profile' }).click();
  await expect(page).toHaveURL(/\/bookings$/);
  await expect(page.getByRole('heading', { name: 'My bookings' })).toBeVisible();
}

test('public catalogue loads from the backend API', async ({ page }) => {
  await page.goto('/catalogue');
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/api/v1/container-types') && candidate.request().method() === 'GET',
  );
  expect(response.status()).toBe(200);
  await expect(page.getByRole('heading', { name: 'Container catalogue' })).toBeVisible();
});

test('public catalogue has no critical or serious accessibility violations', async ({ page }) => {
  await page.goto('/catalogue');
  await expect(page.getByRole('heading', { name: 'Container catalogue' })).toBeVisible();
  const results = await new AxeBuilder({ page }).analyze();
  expect(
    results.violations.filter((violation) =>
      ['critical', 'serious'].includes(violation.impact ?? ''),
    ),
  ).toEqual([]);
});

test('new customers can register and complete their local profile', async ({ page }) => {
  test.skip(
    !registrationPassword,
    'Set E2E_REGISTRATION_PASSWORD to run the Keycloak self-registration workflow.',
  );
  const username = `registration-${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const email = `${username}@example.test`;

  await page.goto('/catalogue');
  await page.getByRole('button', { name: 'Register' }).click();
  await page.getByLabel('First name').fill('Test');
  await page.getByLabel('Last name').fill('Customer');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Username').fill(username);
  await page.getByRole('textbox', { name: 'Password', exact: true }).fill(registrationPassword!);
  await page
    .getByRole('textbox', { name: 'Confirm password', exact: true })
    .fill(registrationPassword!);
  await page.getByRole('button', { name: 'Register' }).last().click();
  await page.waitForURL('http://localhost:4200/**');
  await expect(page.getByRole('link', { name: 'My bookings' })).toBeVisible();

  await completeProfileIfNeeded(page);
});

test('customer can check availability and create a booking', async ({ page }) => {
  test.skip(
    !username || !password,
    'Set E2E_USER_USERNAME and E2E_USER_PASSWORD to run the authenticated customer workflow.',
  );
  await signInAsCustomer(page);
  await page.goto('/catalogue');
  await expect(page.getByRole('heading', { name: 'Container catalogue' })).toBeVisible();
  const bookingLinks = page.getByRole('link', { name: 'Check availability' });
  await expect(bookingLinks.first()).toBeVisible();
  await bookingLinks.first().click();

  const delivery = new Date();
  delivery.setDate(delivery.getDate() + 14);
  const pickup = new Date(delivery);
  pickup.setDate(pickup.getDate() + 2);
  const formatDate = (date: Date) => date.toISOString().slice(0, 10);
  await page.getByLabel('Delivery date').fill(formatDate(delivery));
  await page.getByLabel('Pickup date').fill(formatDate(pickup));
  await page.getByLabel('Street').fill('Test Street');
  await page.getByLabel('House number').fill('1');
  await page.getByLabel('Postal code').fill('10115');
  await page.getByLabel('City').fill('Berlin');
  await page.getByLabel('Country code').fill('DE');
  await page.getByRole('button', { name: 'Check availability' }).click();
  const availability = page.getByRole('status');
  await expect(availability).toBeVisible();
  if ((await availability.textContent())?.includes('not available')) {
    test.skip(true, 'The local backend has no inventory available for the test period.');
  }
  await page.getByRole('button', { name: 'Create booking' }).click();
  await expect(page.getByRole('status')).toContainText('was created');
  await page.getByRole('link', { name: 'View your bookings' }).click();
  await expect(page.getByRole('heading', { name: 'My bookings' })).toBeVisible();
});
