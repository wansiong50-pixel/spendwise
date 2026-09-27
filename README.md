# SpendWise

**A fast, private, offline-first expense tracker for Android.**

SpendWise keeps your entire financial ledger on your device — no account, no
cloud service, no analytics. Log expenses in seconds, see where your money
goes, and let automatic daily backups keep your data safe.

[![Release](https://img.shields.io/github/v/release/wansiong50-pixel/spendwise)](https://github.com/wansiong50-pixel/spendwise/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)

| Home | Activity | Settings & backups |
|:---:|:---:|:---:|
| ![Home](docs/screenshots/home.png) | ![Activity](docs/screenshots/activity.png) | ![Settings](docs/screenshots/settings-backups.png) |

## Features

- **Quick entry** — amount, category, account, merchant, done. Amounts fill
  in from the right (type 1, 2, 3 for RM 1.23), and merchant names are
  auto-canonicalized so "grab", "Grab" and "GRAB " all become one merchant.
- **Home** — total balance across accounts, this month's income, spending
  and budgets, top spending categories, a spending heatmap calendar and
  recent activity.
- **Activity** — every entry for a month or a custom date range, with
  search, account and category filters, and six-month spending and income
  trends.
- **Insights** — month and year views, a 12-month cash-flow chart, and
  category breakdowns.
- **Budgets** — set a monthly limit per category and track progress.
- **Multiple accounts** — cash, bank, e-wallet, credit; archive retired
  accounts without losing history.
- **Transfers and recurring entries** — move money between your own
  accounts without it counting as spending, and let rent, subscriptions and
  salary log themselves.
- **Automatic daily backups** — a background job writes a dated JSON backup
  to a folder you choose. Point it at a cloud-synced folder (Drive,
  OneDrive…) and your data leaves the phone automatically. Keeps the newest
  7, prunes the rest.
- **Manual backup / restore & CSV export** — portable JSON backups (you see
  what a backup holds before restoring it) and CSV export for any month or
  year.
- **Botanical design** — painted and illustrated artwork, a fast cold start,
  and smooth scrolling on low-end devices.

## Privacy

Your data never leaves your device unless *you* put it somewhere: all
storage is a local database, and backups go only to the folder you pick.
There are no accounts, no ads, no trackers, and no data collection.

## Download

Grab the latest APK from the [Releases page](https://github.com/wansiong50-pixel/spendwise/releases)
and install it (you may need to allow installs from unknown sources).

Requires Android 8.0 (API 26) or newer.

## Build from source

Requirements: JDK 17, Android SDK (compileSdk 36). Either open the project
in Android Studio, or use the Gradle wrapper:

```bash
./gradlew :app:testDebugUnitTest   # run unit tests
./gradlew :app:assembleDebug       # build a debug APK
```

The debug APK lands in `app/build/outputs/apk/debug/`.

### Release builds

Signed release builds read credentials from an untracked
`keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Without that file, `assembleRelease` still works but produces an unsigned
APK.

## Architecture

Single-module Kotlin app, plain and dependency-light:

- **UI** — Jetpack Compose, single-activity, with the custom Botanical
  design system in `ui/botanical/` (tokens in `BotanicalTheme.kt`,
  artwork mapping in `BotanicalArt.kt`).
- **Data** — Room (SQLite) with month/year-scoped reactive queries, so
  memory stays flat as the ledger grows. Preferences via DataStore.
- **DI** — manual, via `AppContainer` (no Hilt/Koin).
- **Background work** — WorkManager for the daily auto-backup job.
- **Domain logic** — pure Kotlin in `domain/`, `analytics/` and `backup/`,
  covered by JVM unit tests.

Money is stored as integer cents (`Long`); currency is Malaysian Ringgit
(RM) and dates use the `Asia/Kuala_Lumpur` timezone.

## Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE). Bundled fonts are under the SIL Open Font License — see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
