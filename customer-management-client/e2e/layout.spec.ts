import { test, expect } from './fixtures';

test('redirects from the home page to the customer list', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/customers$/);
});

test('displays the application header with title', async ({ page }) => {
  await page.goto('/customers');
  await expect(page.getByRole('heading', { name: 'Kundenverwaltung', exact: true })).toBeVisible();
});

test('navigates back to the customer list via the header link', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();
  await expect(page).toHaveURL(/\/customers\/\d+$/);

  await page.getByRole('navigation').getByRole('link', { name: 'Kunden' }).click();

  await expect(page).toHaveURL(/\/customers$/);
  await expect(page.getByRole('heading', { name: 'Kunden', exact: true })).toBeVisible();
});

test('navigates from the edit page back to the list via the back link', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();
  await expect(page).toHaveURL(/\/customers\/\d+$/);

  await page.getByRole('link', { name: 'Zurück' }).click();

  await expect(page).toHaveURL(/\/customers$/);
  await expect(page.getByRole('heading', { name: 'Kunden', exact: true })).toBeVisible();
});
