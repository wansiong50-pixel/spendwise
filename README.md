# SpendWise Android Expense Tracker

Native Android MVP for local expense tracking.

## What Is Included

- Kotlin + Jetpack Compose Android app.
- Room database for expenses, categories, and budgets.
- DataStore preference for appearance (light/dark mode).
- Dashboard, transaction list, add/edit expense dialog, and breakdown screens.
- Unit tests for money parsing, expense validation, and spending summaries.

## Build

Use Android Studio or run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\ameer\AppData\Local\Android\Sdk'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```
