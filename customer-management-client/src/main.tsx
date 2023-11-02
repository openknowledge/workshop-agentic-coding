import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider, createRouter } from '@tanstack/react-router';
import { RootRoute as rootRoute } from './routes/root';
import { IndexRoute as indexRoute } from './routes/index';
import { CustomersRoute as customersRoute } from './routes/customers/index';
import { NewCustomerRoute as newCustomerRoute } from './routes/customers/newCustomer';
import { EditCustomerRoute as customerDetailRoute } from './routes/customers/editCustomer';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
    },
  },
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  customersRoute,
  newCustomerRoute,
  customerDetailRoute,
]);

const router = createRouter({ routeTree, context: { queryClient } });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>
);
