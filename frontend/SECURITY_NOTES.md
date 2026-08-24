# Security Notes

Updated on: 2026-08-05

## 1. Current Status

As of 2026-08-05, the frontend dependency audit is mostly clean after the post-migration cleanup.

Resolved through safe upgrades:

- `axios` -> `1.19.0`
- `vite` -> `6.4.3`
- `postcss` -> `8.5.25`

Remaining audit warning:

- `react-router-dom`
- `react-router`

## 2. Remaining Audit Finding

`npm audit` still reports a high-severity advisory on `react-router` / `react-router-dom`.

Observed state on 2026-08-05:

- Current installed version: `react-router-dom@7.18.2`
- `npm audit` flags versions in the `7.12.0 - 8.2.0` range
- `npm audit --force` suggests downgrading to `7.11.0`
- Downgrading to `7.11.0` introduces a different set of React Router high-severity advisories

Conclusion:

- There is no stable npm release currently available in this project that removes the audit warning without reintroducing other known React Router advisories

## 3. Project Exposure Assessment

This repository currently uses React Router in a limited browser-only mode.

Confirmed usage:

- `BrowserRouter`
- `Routes`
- `Route`
- `Link`
- `NavLink`
- `Navigate`
- `useNavigate`

Not used in the current codebase:

- React Router RSC mode
- SSR routing
- `StaticRouter`
- `RouterProvider`
- `createBrowserRouter`
- `createStaticRouter`
- `ScrollRestoration`
- server actions / data router form actions

Risk interpretation:

- The current audit warning appears broader than this repository's actual runtime usage
- The codebase does not currently exercise the highest-risk React Router features referenced by the latest audit output
- The warning should still be tracked until an upstream stable fix is published

## 4. Development Hardening

The development server now supports explicit host binding through `VITE_APP_HOST`.

Recommended default for local-only development:

- `VITE_APP_HOST=127.0.0.1`

Use `0.0.0.0` only when LAN access is intentionally needed.

## 5. Recommended Follow-Up

1. Re-check `npm audit` when a new stable `react-router-dom` version is published
2. Upgrade immediately once a stable release clears the current advisory set
3. Keep avoiding unnecessary SSR / RSC / data-router expansion until dependency status is cleaner
4. Prefer local-only host binding during development unless cross-device testing is required
