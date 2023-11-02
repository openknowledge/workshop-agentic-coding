import { createRootRouteWithContext, Link, Outlet } from '@tanstack/react-router';
import type { QueryClient } from '@tanstack/react-query';

interface RouterContext {
  queryClient: QueryClient;
}

export const RootRoute = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
});

export function RootLayout() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Kundenverwaltung</h1>
        <nav>
          <Link to="/customers">Kunden</Link>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
