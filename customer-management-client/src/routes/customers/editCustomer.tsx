import { useState } from 'react';
import { createRoute, useNavigate, Link } from '@tanstack/react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import { CustomerForm } from '../../features/customers/components/CustomerForm';
import {
  customerQueryOptions,
  useUpdateCustomer,
} from '../../features/customers/api/customerQueries';
import { RootRoute as rootRoute } from '../root';

export const EditCustomerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/customers/$customerId',
  loader: ({ context: { queryClient }, params: { customerId } }) =>
    queryClient.ensureQueryData(customerQueryOptions(Number(customerId))),
  component: EditCustomerPage,
});

export function EditCustomerPage() {
  const { customerId } = EditCustomerRoute.useParams();
  const id = Number(customerId);
  const navigate = useNavigate();
  const { data: customer } = useSuspenseQuery(customerQueryOptions(id));
  const updateMutation = useUpdateCustomer(id);
  const [error, setError] = useState<string | null>(null);

  return (
    <div>
      <div className="page-header">
        <h2>Kunde bearbeiten</h2>
        <Link to="/customers" className="btn">
          Zurück
        </Link>
      </div>
      {error && <div className="error-message">{error}</div>}
      <CustomerForm
        defaultValues={customer}
        onSubmit={(data) => {
          setError(null);
          updateMutation.mutate(data, {
            onSuccess: () => navigate({ to: '/customers' }),
            onError: (err) => setError(err.message),
          });
        }}
        isPending={updateMutation.isPending}
        submitLabel="Speichern"
      />
    </div>
  );
}
