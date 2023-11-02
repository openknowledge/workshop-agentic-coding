import { useForm } from '@tanstack/react-form';
import { FormField } from '../../../components/FormField';
import { customerSchema, type Customer } from '../../../domain/Customer';

interface CustomerFormProps {
  defaultValues?: Customer;
  onSubmit: (data: Customer) => void;
  isPending: boolean;
  submitLabel: string;
}

export function CustomerForm({
  defaultValues,
  onSubmit,
  isPending,
  submitLabel,
}: CustomerFormProps) {
  const form = useForm({
    defaultValues: {
      name: {
        firstName: defaultValues?.name?.firstName ?? '',
        lastName: defaultValues?.name?.lastName ?? '',
      },
      email: defaultValues?.email ?? '',
      phoneNumber: defaultValues?.phoneNumber,
      birthDate: defaultValues?.birthDate,
    },
    onSubmit: async ({ value }) => {
      onSubmit(value);
    },
    validators: {
      onChange: customerSchema,
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        e.stopPropagation();
        form.handleSubmit();
      }}
      className="customer-form"
    >
      <form.Field name="name.firstName">
        {(field) => <FormField field={field} label="Vorname" required />}
      </form.Field>

      <form.Field name="name.lastName">
        {(field) => <FormField field={field} label="Nachname" required />}
      </form.Field>

      <form.Field name="email">
        {(field) => <FormField field={field} label="E-Mail" type="email" required />}
      </form.Field>

      <form.Field name="phoneNumber">
        {(field) => <FormField field={field} label="Telefonnummer" type="tel" />}
      </form.Field>

      <form.Field name="birthDate">
        {(field) => <FormField field={field} label="Geburtsdatum" type="date" />}
      </form.Field>

      <div className="form-actions">
        <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]}>
          {([canSubmit, isSubmitting]) => (
            <button
              type="submit"
              disabled={!canSubmit || isSubmitting || isPending}
              className="btn btn-primary"
            >
              {isPending ? 'Wird gespeichert...' : submitLabel}
            </button>
          )}
        </form.Subscribe>
      </div>
    </form>
  );
}
