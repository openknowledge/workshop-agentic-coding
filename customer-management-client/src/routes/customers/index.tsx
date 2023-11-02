import { createRoute, Link } from '@tanstack/react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import { customersQueryOptions } from '../../features/customers/api/customerQueries';
import { RootRoute as rootRoute } from '../root';
import { CustomerTable } from '../../features/customers/components/CustomerTable';

export const CustomersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/customers',
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(customersQueryOptions),
  component: CustomerListPage,
});

export function CustomerListPage() {
  const { data: customers } = useSuspenseQuery(customersQueryOptions);

  return (
    <div>
      <div className="page-header">
        <h2>Kunden</h2>
        <Link to="/customers/new" className="btn">
          Neuen Kunden anlegen
        </Link>
      </div>
      <CustomerTable customers={customers} />
    </div>
  );
}
