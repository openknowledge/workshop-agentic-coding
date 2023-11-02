import { test, expect } from '../fixtures';

test('displays the customer list', async ({ page }) => {
  await page.goto('/customers');
  await expect(page.getByRole('heading', { name: 'Kunden', exact: true })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();
});

test('navigates to the edit page of a customer', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();
  await expect(page).toHaveURL(/\/customers\/\d+$/);
  await expect(page.getByRole('heading', { name: 'Kunde bearbeiten' })).toBeVisible();
});

test('saves changes and returns to the customer list', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();

  const firstNameInput = page.getByLabel(/Vorname/);
  await firstNameInput.clear();
  await firstNameInput.fill('Max');

  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(page).toHaveURL('/customers');
});

test('filters the customer list via the search field', async ({ page }) => {
  await page.goto('/customers');
  await expect(page.locator('tbody')).toContainText('Mustermann');
  await expect(page.locator('tbody')).toContainText('Musterfrau');

  await page.getByPlaceholder('Suchen...').fill('Erika');

  await expect(page.locator('tbody')).toContainText('Musterfrau');
  await expect(page.locator('tbody')).not.toContainText('Mustermann');
});

test('sorts the table when clicking a column header', async ({ page }) => {
  await page.goto('/customers');
  await page.getByPlaceholder('Suchen...').fill('Muster');

  const nachnameHeader = page.getByRole('columnheader', { name: /Nachname/ });

  await nachnameHeader.click();
  await expect(nachnameHeader).toContainText('▲');
  await expect(page.locator('tbody tr').first()).toContainText('Musterfrau');

  await nachnameHeader.click();
  await expect(nachnameHeader).toContainText('▼');
  await expect(page.locator('tbody tr').first()).toContainText('Mustermann');
});

test('shows a hint when the customer list is empty', async ({ page }) => {
  await page.route('**/api/customers', (route) => route.fulfill({ json: [] }));

  await page.goto('/customers');

  await expect(page.getByRole('cell', { name: 'Keine Kunden vorhanden' })).toBeVisible();
});

test('disables the pagination buttons when there is only one page', async ({ page }) => {
  await page.goto('/customers');

  await expect(page.getByRole('button', { name: 'Zurück' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Weiter' })).toBeDisabled();
});

test('cancels the deletion when the confirmation is declined', async ({ page }) => {
  await page.goto('/customers');
  await expect(page.locator('tbody')).toContainText('Mustermann');

  page.once('dialog', (dialog) => dialog.dismiss());
  await page.getByRole('row', { name: /Max/ }).getByRole('button', { name: 'Löschen' }).click();

  await expect(page.locator('tbody')).toContainText('Mustermann');
});

test('creates a new customer and deletes them again', async ({ page }) => {
  const uniqueEmail = `e2e.create.delete+${Date.now()}-${Math.random().toString(36).slice(2)}@example.com`;

  await page.goto('/customers/new');
  await page.getByLabel(/Vorname/).fill('E-to-E');
  await page.getByLabel(/Nachname/).fill('Tester');
  await page.getByLabel(/E-Mail/).fill(uniqueEmail);
  await page.getByRole('button', { name: 'Anlegen' }).click();

  await expect(page).toHaveURL(/\/customers$/);

  await page.getByPlaceholder('Suchen...').fill(uniqueEmail);
  const newRow = page.getByRole('row', { name: new RegExp(`E-to-E\\s+Tester`) });
  await expect(newRow).toBeVisible();

  page.once('dialog', (dialog) => dialog.accept());
  await newRow.getByRole('button', { name: 'Löschen' }).click();

  await expect(page.locator('tbody')).not.toContainText(uniqueEmail);
});

test('prefills the edit form with the customer data', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('row', { name: /Erika/ }).getByRole('link', { name: 'Bearbeiten' }).click();

  await expect(page.getByLabel(/Vorname/)).toHaveValue('Erika');
  await expect(page.getByLabel(/Nachname/)).toHaveValue('Musterfrau');
  await expect(page.getByLabel(/E-Mail/)).toHaveValue('erika.musterfrau@openknowledge.de');
  await expect(page.getByLabel(/Telefonnummer/)).toHaveValue('+49 987 6543210');
  await expect(page.getByLabel(/Geburtsdatum/)).toHaveValue('1985-11-23');
});

test('shows an error message when saving fails', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();

  await page.route('**/api/customers/*', (route) => {
    if (route.request().method() === 'PUT') {
      return route.fulfill({ status: 500, body: 'Internal Server Error' });
    }
    return route.fallback();
  });

  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(page.getByText('Kunde konnte nicht aktualisiert werden')).toBeVisible();
  await expect(page).toHaveURL(/\/customers\/\d+$/);
});

test('shows an error message for an invalid email address', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();

  const emailInput = page.getByLabel(/E-Mail/);
  await emailInput.clear();
  await emailInput.fill('keine-email');
  await emailInput.blur();

  await expect(page.getByText('Ungültige E-Mail-Adresse')).toBeVisible();
});

test('shows an error message for an empty last name', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();

  const lastNameInput = page.getByLabel(/Nachname/);
  await lastNameInput.clear();
  await lastNameInput.blur();

  await expect(page.getByText('Nachname ist erforderlich')).toBeVisible();
});

test('disables the save button when the form is invalid', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Bearbeiten' }).first().click();

  const emailInput = page.getByLabel(/E-Mail/);
  await emailInput.clear();
  await emailInput.fill('keine-email');
  await emailInput.blur();

  await expect(page.getByRole('button', { name: 'Speichern' })).toBeDisabled();
});

test('paginates to the next page', async ({ page }) => {
  const manyCustomers = Array.from({ length: 12 }, (_, index) => ({
    firstName: `Vorname${index + 1}`,
    lastName: `Nachname${index + 1}`,
    email: `kunde${index + 1}@example.com`,
    self: { href: `/api/customers/${index + 1}` },
  }));
  await page.route('**/api/customers', (route) => route.fulfill({ json: manyCustomers }));

  await page.goto('/customers');
  await expect(page.locator('tbody tr')).toHaveCount(10);
  await expect(page.getByText('Seite 1 von 2')).toBeVisible();

  await page.getByRole('button', { name: 'Weiter' }).click();

  await expect(page.locator('tbody tr')).toHaveCount(2);
  await expect(page.getByText('Seite 2 von 2')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Weiter' })).toBeDisabled();

  await page.getByRole('button', { name: 'Zurück' }).click();

  await expect(page.locator('tbody tr')).toHaveCount(10);
  await expect(page.getByText('Seite 1 von 2')).toBeVisible();
});

test('sorts the table by first name and by email', async ({ page }) => {
  await page.goto('/customers');
  await page.getByPlaceholder('Suchen...').fill('Muster');

  const vornameHeader = page.getByRole('columnheader', { name: /Vorname/ });
  await vornameHeader.click();
  await expect(vornameHeader).toContainText('▲');
  await expect(page.locator('tbody tr').first()).toContainText('Erika');

  const emailHeader = page.getByRole('columnheader', { name: /E-Mail/ });
  await emailHeader.click();
  await expect(emailHeader).toContainText('▲');
  await expect(page.locator('tbody tr').first()).toContainText('erika.musterfrau@openknowledge.de');

  await emailHeader.click();
  await expect(emailHeader).toContainText('▼');
  await expect(page.locator('tbody tr').first()).toContainText('max.mustermann@openknowledge.de');
});

test('shows an error screen when loading the customer list fails', async ({ page }) => {
  await page.route('**/api/customers', (route) =>
    route.fulfill({ status: 500, body: 'Internal Server Error' })
  );

  await page.goto('/customers');

  await expect(page.getByText('Something went wrong!')).toBeVisible();
  await expect(page.getByText('Kunden konnten nicht geladen werden')).toBeVisible();
  await expect(page.getByRole('table')).toHaveCount(0);
});

test('shows an error screen when a customer does not exist', async ({ page }) => {
  await page.goto('/customers/9999');

  await expect(page.getByText('Something went wrong!')).toBeVisible();
  await expect(page.getByText('Kunde nicht gefunden')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Speichern' })).toHaveCount(0);
});

test('leaves the table unchanged when the deletion fails', async ({ page }) => {
  await page.route('**/api/customers/*', (route) => {
    if (route.request().method() === 'DELETE') {
      return route.fulfill({ status: 500, body: 'Internal Server Error' });
    }
    return route.fallback();
  });

  await page.goto('/customers');
  await expect(page.locator('tbody tr').first()).toBeVisible();
  const rowsBefore = await page.locator('tbody tr').count();

  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('row', { name: /Max/ }).getByRole('button', { name: 'Löschen' }).click();

  await expect(page.getByRole('button', { name: 'Löschen' }).first()).toBeEnabled();
  await expect(page.locator('tbody tr')).toHaveCount(rowsBefore);
});

test('opens the form for a new customer via the link in the customer list', async ({ page }) => {
  await page.goto('/customers');
  await page.getByRole('link', { name: 'Neuen Kunden anlegen' }).click();

  await expect(page).toHaveURL(/\/customers\/new$/);
  await expect(page.getByRole('heading', { name: 'Neuer Kunde' })).toBeVisible();
  await expect(page.getByLabel(/Vorname/)).toHaveValue('');
  await expect(page.getByLabel(/Nachname/)).toHaveValue('');
  await expect(page.getByLabel(/E-Mail/)).toHaveValue('');
});

test('validates the email address in the new customer form', async ({ page }) => {
  await page.goto('/customers/new');

  const emailInput = page.getByLabel(/E-Mail/);
  await emailInput.fill('keine-email');
  await emailInput.blur();

  await expect(page.getByText('Ungültige E-Mail-Adresse')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Anlegen' })).toBeDisabled();
});

test('returns from the new customer form to the customer list without saving', async ({ page }) => {
  await page.goto('/customers/new');
  await page.getByRole('link', { name: 'Zurück' }).click();

  await expect(page).toHaveURL(/\/customers$/);
  await expect(page.getByRole('heading', { name: 'Kunden', exact: true })).toBeVisible();
});

test('shows an error message when the creation fails', async ({ page }) => {
  await page.route('**/api/customers', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 500, body: 'Internal Server Error' });
    }
    return route.fallback();
  });

  await page.goto('/customers/new');
  await page.getByLabel(/Vorname/).fill('Neu');
  await page.getByLabel(/Nachname/).fill('Kunde');
  await page.getByLabel(/E-Mail/).fill('neu@example.com');

  await page.getByRole('button', { name: 'Anlegen' }).click();

  await expect(page.getByText('Kunde konnte nicht angelegt werden')).toBeVisible();
  await expect(page).toHaveURL(/\/customers\/new$/);
});
