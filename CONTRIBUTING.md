# Contributing to Publishly Kit

Thanks for helping. This is a small library with one rule above all others: an app that adds it should get working, well-behaved features and nothing else, so no analytics, no ad SDKs, no permissions beyond `INTERNET`.

## Set up

- Android Studio with AGP 9.0.1, JDK 17, compileSdk 36. The versions are pinned in [gradle/libs.versions.toml](gradle/libs.versions.toml); don't bump them past what the README's "Add it" section states without a note in the PR.
- `./gradlew :kit:testDebugUnitTest` runs the unit tests. They use real data checked into the repo (update JSON, the promo template, saved Play pages): add the same kind of test for anything you change in the parsers or the rules.
- `./gradlew :sample:installDebug` puts the sample app on a phone or emulator. Every feature is behind a button there; use it to see your change.

## What a good change looks like

- **Plain Kotlin at the core.** `kit` has no Compose dependency, so View and Java apps can use it. Compose wrappers go in `kit-compose`.
- **Both worlds.** A new screen or prompt comes with a Compose version and a plain-View version, both taking the app's colours (`KitTheme`).
- **Rules are pure functions.** Anything that decides *when* (ask for a review, show the featured app, which route the update takes) lives in a small function with no Android types, with a unit test next to it. See `ReviewAsk.isDue`, `Updates.route`, `AppPromos.featuredDue`.
- **Fails quietly.** No network, no Play, a broken file: the app carries on and logs one line. Nothing in the kit may crash the host app.
- **Write for the reader.** Comments and README text in plain English; say why, not what the code already says.

## Sending it

Open a pull request against `main` with a short description of the change and how you tested it (a screenshot from the sample app helps). Commit messages are short imperative sentences ("Add a Wrap layout to MoreAppsRow"). CI runs the tests and builds the sample on every PR.

## Licence

By contributing you agree that your work is released under the [Apache 2.0 licence](LICENSE), like the rest of the project.
