import { createRoute, Navigate } from '@tanstack/react-router';
import { RootRoute as rootRoute } from './root';

export const IndexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => <Navigate to="/customers" />,
});
