import { expect, test } from '@playwright/test';

const username = process.env.E2E_ADMIN_USERNAME;
const password = process.env.E2E_ADMIN_PASSWORD;

test('administrator can open the management workspace and load bookings', async ({ page }) => {
  test.skip(
    !username || !password,
    'Set E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD to run the authenticated administrator workflow.',
  );
  await page.goto('/catalogue');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.getByLabel(/username/i).fill(username ?? '');
  await page.getByLabel(/password/i).fill(password ?? '');
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL('http://localhost:4200/**');
  const bookingsResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/v1/admin/bookings') && response.request().method() === 'GET',
  );
  await page.getByRole('link', { name: 'Administration' }).click();
  await page.getByRole('tab', { name: 'Bookings' }).click();
  expect((await bookingsResponse).status()).toBe(200);
  await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Customers' })).toBeVisible();
  await page.getByRole('tab', { name: 'Invoices' }).click();
  await expect(page.getByRole('heading', { name: 'Create invoice' })).toBeVisible();
});
