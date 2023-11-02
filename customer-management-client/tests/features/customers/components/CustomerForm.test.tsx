import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CustomerForm } from '../../../../src/features/customers/components/CustomerForm';

test('shows error message if required input field is empty', async () => {
  const user = userEvent.setup();
  render(<CustomerForm onSubmit={vi.fn()} isPending={false} submitLabel="Speichern" />);

  const firstName = screen.getByLabelText(/Vorname/);
  await user.type(firstName, 'A');
  await user.clear(firstName);
  await user.tab();

  expect(await screen.findByText(/Vorname ist erforderlich/i)).toBeInTheDocument();
});

test('show pending state and deactivates button on submission', () => {
  render(<CustomerForm onSubmit={vi.fn()} isPending={true} submitLabel="Speichern" />);

  const button = screen.getByRole('button', { name: 'Wird gespeichert...' });
  expect(button).toBeDisabled();
});
