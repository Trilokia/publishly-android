# Changelog

## 1.1.0

- **Changed default:** the "More apps" row and list now keep the apps the phone already has, after the others, with an "Installed · Thanks!" label and an Open button that launches the app (the Play page when it has no launcher screen). `showInstalled = false` on `MoreAppsRow` / `MoreAppsList` keeps the old behaviour. `AppPromos.apps()` and the featured dialog are unchanged: the featured dialog never promotes an installed app.
- `AppPromos.allApps()` returns every feed app with its installed flag, `AppPromos.launch()` opens an installed one; `arrange()` is the ordering rule, with tests.
- The `row` / `item` overrides of the lists now receive the installed flag.
- `MoreAppsDefaults.style()`: `showInstalledLabel`, `installedContainer`, `installedContent`.
- The lists can carry your name: "More apps from Trilokia Inc." from `AppPromos(developerName = …)` or a top-level `"developer"` in the feed, "More apps from us" without one. `rememberMoreAppsTitle(promos)` and `promos.title()` give the same title for your own screens; the View screen uses it too.
- The review prompt's bottom sheet puts its main button ("Rate on Play" / "Send feedback") on its own full-width line, so it no longer wraps at larger font sizes.
- Hindi translations of every user-facing string (`values-hi`).

## 1.0.0

- First release: in-app updates (Play first, a JSON file as the fallback), a review prompt that asks you first with animated stars and a feedback mail for low ratings, your other apps from a JSON feed, What's new from Play / JSON / GitHub / bundled notes, open source licences from the build, one theme for Compose and View screens. Dialog, bottom-sheet and snackbar prompts; list, grid, cards and compact layouts; a style object for every element.
