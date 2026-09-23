# Frontend

React + TypeScript (Vite), strict mode. See `../docs/06-frontend/` for the full spec.

## Run locally

Requires the backend running separately (`../backend/README.md`) — the Vite dev server proxies `/api` to `http://localhost:8080` so the browser never needs CORS configuration.

```bash
npm install
npm run dev
```

## Build / typecheck

```bash
npm run build
```

Runs `tsc -b` (strict mode) then `vite build`. Both must pass with no errors before any change here is done.

## Layout

- `src/api/client.ts` — the single API client layer; every request goes through it so the `{data, meta}` / `{error}` response envelope is parsed in one place.
- `src/pages/` — one component per screen in `../docs/06-frontend/screen-specification.md`. Unbuilt screens use `ComingSoonPage` as a placeholder.
- `src/components/Layout.tsx` — navigation shell matching `../docs/06-frontend/ui-ux-specification.md`.
