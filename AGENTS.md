# Agent Instructions

## Visual Issues

For all visual issues (layout, styling, display errors, rendering, UI behavior in the browser), the Playwright MCP **must** always be used:

- Open the affected page in the browser with the Playwright MCP before making any changes.
- Analyze the problem based on the actual rendering (screenshot or accessibility snapshot), not just the source code.
- After every change, check again with the Playwright MCP whether the problem has been fixed.
- Do not rely on assumptions about how the application looks; verify it visually.

## Docker Compose Setup

For the visual check, the Docker Compose setup can be used to run the application (database, Flyway migration, WireMock and the server). Start it with:

```bash
docker compose up --build
```

The server is then available at http://localhost:8080.
