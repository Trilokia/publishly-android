# Changelog

## 2.1.0

The one change a 2.0 Compose app must make: if it shows `OpenSourceScreen`, add `implementation("com.github.Trilokia.publishly-android:kit-oss-compose:v2.1.0")` (it brings `kit-oss` with it). View apps change nothing. Four fixes to the licence list, found when Gamers GLTool Pro (a Java, View app) adopted 2.0.1:

- **Breaking for Compose apps:** `OpenSourceScreen` (and `OpenSourceStyle`, `OpenSourceLayout`, `LicenceDialog`, `rememberLibraries`) moved to a new `kit-oss-compose` module, with the same package name. `kit-oss` keeps `OpenSource`, `OpenSourceActivity` and AboutLibraries core only, with no Compose: before, it pulled Compose UI, Material 3 and AboutLibraries' Compose list in at runtime even for apps that only call `OpenSource.open()`, so the AboutLibraries plugin listed all those Compose artifacts in a View app's licences (R8 strips every Compose class from the release, so the list named libraries the app didn't ship).
- **Licence text reflowed for a phone.** SPDX texts are hard-wrapped at about 72 columns, so every line wrapped again into ragged half lines. `LicenceText.reflow` (public, pure Kotlin) joins each paragraph into one line and keeps titles, "Version 3, 29 June 2007", addresses and lists of short lines as they are; no word is lost or reordered. Tested on every text of a real `aboutlibraries.json` (Apache-2.0, MIT, GPL-2.0 with the Classpath Exception, LGPL-3.0, OFL-1.1). Both screens use it.
- **The website link is inside the scrolling text,** its first line, in both dialogs. As a neutral button under OK it was cut off by the dialog's rounded bottom edge on long texts (LGPL + GPL, 42k characters, API 35, 1080x2400). Checked with MIT and LGPL at font scale 1.0 and 1.3, light and dark.
- **The list sorts by name as people read it:** case-insensitive and locale-aware (`java.text.Collator`, PRIMARY strength), tie-broken by `uniqueId`, in `OpenSource.libraries()` (so `OpenSourceActivity`, `OpenSourceScreen` and the By licence groups alike). Before, `libadb-android`, `spake2-java` and `sun-security-android` came after every capitalised name. `OpenSource.byName()` and `OpenSource.nameOrder()` are the comparators, for your own UI.
- Sample: depends on `kit-oss-compose`.

## 2.0.1

- Each published module's POM carries its own name ("Publishly Kit", "Publishly Kit Compose", "Publishly Kit OSS"), a description, the project URL, the Apache 2.0 licence and the source link. Before, JitPack filled every `<name>` with "Trilokia/publishly-android", so apps that build their licence list from POMs showed the library two or three times under one name. No code changes.

## 2.0.0

The one change a 1.x app must make: if it shows the open source licence list, add `implementation("com.github.Trilokia.publishly-android:kit-oss:v2.0.0")`. Everything else compiles and looks as before.

- **Breaking:** `OpenSource`, `OpenSourceActivity` and `OpenSourceScreen` moved to a new `kit-oss` module, with the same package names, so `kit` no longer carries AboutLibraries for apps with a licence screen of their own. The three `kit_open_source*` / `kit_licence_unknown` strings moved with them. `KitScreen` (the View screens' theme and top bar) is public now, so a second module, or your own View screen, can use it.
- **Draw the prompts yourself:** every prompt takes an optional `content` lambda, and with one the kit draws nothing of its own, only the logic (when to show, what each button does, marking shown and dismissed): `ReviewPrompt(reviewAsk) { s -> … }` (also `ReviewPromptDialog`, `ReviewPromptSheet`) with a `ReviewPromptState` (`stars`, `setStars()`, `title`, `message`, `hint`, `confirmLabel`, `confirm()`, `later()`, `never()`, `starLabel(n)`); `UpdateFlow(updates) { info, cancelable, onUpdate, onLater -> … }` and `UpdatePrompt(info, onDismiss = …) { … }`; `UpdateReadyBar(updates) { onRestart -> … }`; `FeaturedPromoDialog(promos) { featured, onGet, onNotNow -> … }`; `WhatsNewDialog(whatsNew) { notes, onClose -> … }`. The state holders behind them, for a prompt that isn't a dialog: `rememberReviewPromptState`, `rememberUpdateFlowState`, `rememberFeaturedPromoState` (with `never()`), `rememberWhatsNewDialogState` (with `show(notes)` for a Settings row). Without a lambda nothing changes; the 1.x calls (`UpdateFlow(updates) { settled = true }`, `UpdatePrompt(info) { … }`) still pick the old functions.
- **The kit's dialogs in your style:** every prompt takes `style = PromptDefaults.style(…)` (container, title and body colours, accent for buttons and stars, shape, type; the sheet and the restart bar have their own entries), the way the lists take `MoreAppsDefaults.style(…)`. With no arguments it is the look the kit has always had.
- **One update state:** `Updates.state` and `PlayUpdates.state` are `StateFlow<UpdateState>` (`Idle`, `Checking`, `UpToDate`, `Downloading(bytes, total)` with `fraction`, `Downloaded`, `ServerUpdate(info)`, `Failed(reason)`), Play first and the JSON file when Play can't answer, download progress included. `suspend fun checkNow(): UpdateState` on both, for a "Check for updates" row (it waits for Play's screen to close); `JsonUpdates.checkState()`. The `downloaded` flow and the callback `checkNow { }` work as before.
- **Promo icons offline:** the feed's `icon` can be a file in the app, `"asset:promo/x.webp"`, with `iconUrl` as the web backup (`PromoApp.iconFallbackUrl`). `rememberPromoIcon(app)`, `PromoIcon(app)` and `PromoIcons.load(context, app)` try the sources in order; an icon already in memory is there on the first frame. Server feeds can keep web icons only.
- **No INTERNET permission:** an app that removes it (`tools:node="remove"`) gets no fetches and no warnings; the permission is read once per process and the update file, promo feed, icons and what's-new sources answer from their bundled copies and caches. Play's update and review still work.
- **Text:** every piece of text comes from a `kit_*` string resource, in English and Hindi, and the README lists them with how to override. New `kit_review_star` ("%1$d of 5 stars") labels each star for TalkBack; the licence dialog's OK is `kit_ok` instead of Android's.
- Sample: a "Custom look" screen (Home → Draw it yourself) with a switch between the kit's dialogs in a style of the sample's own and every prompt drawn by the app through the slots, plus an update-state card and a rating card built from the state holders; the bundled feed ships the real icons as assets.

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
