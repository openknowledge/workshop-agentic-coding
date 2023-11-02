import { render, screen } from '@testing-library/react';
import type { AnyFieldApi } from '@tanstack/react-form';
import { FormField } from '../../src/components/FormField';

function makeField(errors: unknown[] = []): AnyFieldApi {
  return {
    name: 'testField',
    state: {
      value: '',
      meta: { errors, isTouched: false, isDirty: false, isValidating: false },
    },
    handleChange: vi.fn(),
    handleBlur: vi.fn(),
  } as unknown as AnyFieldApi;
}

test('displays string error message directly', () => {
  render(<FormField field={makeField(['Pflichtfeld'])} label="Testfeld" />);

  expect(screen.getByText('Pflichtfeld')).toBeInTheDocument();
});

test('extracts readable message from object error', () => {
  render(<FormField field={makeField([{ message: 'Ungültiger Wert' }])} label="Testfeld" />);

  expect(screen.getByText('Ungültiger Wert')).toBeInTheDocument();
});

test('concatenates multiple errors to one string', () => {
  render(
    <FormField field={makeField(['Erforderlich', { message: 'Zu kurz' }])} label="Testfeld" />
  );

  expect(screen.getByText('Erforderlich, Zu kurz')).toBeInTheDocument();
});

test('shows an asterisks at the required input fields', () => {
  render(<FormField field={makeField()} label="Vorname" required />);

  expect(screen.getByText(/Vorname \*/)).toBeInTheDocument();
});

test('does not show an asterisks at an optional field', () => {
  render(<FormField field={makeField()} label="Telefonnummer" />);

  expect(screen.queryByText(/\*/)).not.toBeInTheDocument();
});
