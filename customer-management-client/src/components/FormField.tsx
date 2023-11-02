import type { AnyFieldApi } from '@tanstack/react-form';

interface FormFieldProps {
  field: AnyFieldApi;
  label: string;
  required?: boolean;
  type?: string;
}

export function FormField({ field, label, required = false, type = 'text' }: FormFieldProps) {
  return (
    <div className="form-field">
      <label htmlFor={field.name}>
        {label}
        {required && ' *'}
      </label>
      <input
        id={field.name}
        type={type}
        value={field.state.value ?? ''}
        onChange={(e) => field.handleChange(e.target.value)}
        onBlur={field.handleBlur}
      />
      {field.state.meta.errors.length > 0 && (
        <span className="field-error">
          {field.state.meta.errors.map((e) => (typeof e === 'string' ? e : e.message)).join(', ')}
        </span>
      )}
    </div>
  );
}
