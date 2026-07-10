# Contributing to SpendWise

Thanks for your interest! SpendWise is a small personal-finance app and
contributions are welcome.

## Getting started

1. Fork and clone the repo.
2. Open in Android Studio (or use the Gradle wrapper directly — see the
   README's Build section). Requires JDK 17 and the Android SDK
   (compileSdk 36).
3. Run the unit tests before and after your change:

   ```
   ./gradlew :app:testDebugUnitTest
   ```

## Guidelines

- **Keep the local-first promise.** SpendWise stores everything on-device.
  Don't add analytics, tracking, or network calls that ship user data
  anywhere.
- **Match the existing style.** Kotlin, Jetpack Compose, manual DI via
  `AppContainer` — no new frameworks without discussion.
- **Domain logic gets unit tests.** Pure logic (parsing, validation,
  aggregation) lives in `domain/` / `analytics/` / `backup/` and is covered
  by JVM tests in `app/src/test/`.
- **One change per pull request**, with a short description of what and why.

## Reporting bugs / requesting features

Open a GitHub issue with steps to reproduce (for bugs) or the use case
you're trying to solve (for features).
