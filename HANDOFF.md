# SpendWise Android — Handoff

> Pick this up cold. Everything you need is in here.

## What it is

Native Android expense tracker. Kotlin + Jetpack Compose, single-activity, local-only (no cloud, no auth). Brand: **SpendWise**, **Plum & Coral** palette, dark/light mode toggle. Currency hardcoded to MYR (RM).

Design source of truth: `C:\Users\ameer\Downloads\Expense App-handoff\expense-app\project\SpendWise.html` and `screens.jsx`. The Compose UI is a port of that React prototype.

## Tech stack

- **Kotlin** 1.9.x, **Compose** BOM 2024.06.00, **Material 3**
- **Room** 2.6.1 (KAPT) — single DB at `expense_tracker.db`
- **DataStore Preferences** — only used for appearance (light/dark)
- **Navigation Compose** 2.7.7
- `kotlinx-coroutines-android` 1.8.1
- minSdk 26, targetSdk 36, compileSdk 36, JVM target 17

No DI framework — manual wiring through `AppContainer`.

## Project layout

```
app/src/main/java/com/spendwise/app/
├── MainActivity.kt                         # Single activity, sets Compose root + theme
├── ExpenseTrackerApplication.kt            # Holds AppContainer
├── AppContainer.kt                         # Manual DI: DB, repository, analyzer, prefs
├── analytics/
│   ├── MonthlySpendingSummary.kt           # data class — total, categoryTotals, dailyTotals, etc.
│   └── SpendingAnalyzer.kt                 # pure function: List<Expense> -> summary
├── data/
│   ├── AppearancePreferenceMapper.kt
│   ├── AppearancePreferenceStore.kt        # DataStore for dark mode
│   ├── DefaultExpenseRepository.kt
│   ├── ExpenseDao.kt                       # ExpenseDao + CategoryDao
│   ├── ExpenseDatabase.kt                  # @Database version 4 (was 3)
│   ├── ExpenseEntity.kt                    # ExpenseEntity, CategoryEntity, BudgetEntity
│   ├── ExpenseRepository.kt
│   └── Mappers.kt                          # entity <-> domain
├── domain/
│   ├── ExpenseValidator.kt
│   ├── Models.kt                           # Category, Expense, CategoryTotal, Budget
│   └── MoneyFormatter.kt                   # parseToCents / formatCents
└── ui/
    ├── DashboardScreen.kt                  # Home tab screen
    ├── ExpenseTrackerApp.kt                # Nav host + remaining Breakdown/Add Expense/shared composables
    ├── ExpenseTrackerViewModel.kt          # single VM for the whole app
    ├── TransactionsScreen.kt               # Transactions tab screen
    └── theme/
        ├── Color.kt                        # SpendWise palette tokens (incl. AppSWPrimary, OnCoral, etc.)
        ├── Theme.kt                        # SpendWiseColors data class, light/dark instances
        └── Type.kt
```

**Important:** `ExpenseTrackerApp.kt` is still large. Dashboard and Transactions have been split out, but Breakdown, Add/Edit Expense, and most shared composables still live there. Keep refactoring incremental; don't try a big-bang split.

## Recent changes (this session)

### 1. AI integration was completely removed
The app previously had MediaPipe Tasks GenAI + Gemma on-device LLM for "AI Insights." It's gone:
- Deleted: `ai/` package (5 files), `data/AiPreferenceStore.kt`, `test/.../AiPromptBuilderTest.kt`, `test/.../InsightPreviewCardLayoutTest.kt`
- Removed dependency `com.google.mediapipe:tasks-genai:0.10.27` from `app/build.gradle.kts`
- Cleared MediaPipe keep rule in `app/proguard-rules.pro`
- Domain types removed: `AiModel`, `AiModelStatus`, `AiModelEntity`, `AiModelDao`
- Bottom nav reduced from **5 tabs → 3 tabs**: Home / Transactions / Breakdown
- Room DB bumped **v3 → v4** with `MIGRATION_3_4` that drops the `ai_models` table while preserving expenses/categories
- Composables removed: `InsightsScreen`, `ModelsScreen`, `AiReadinessCard`, `PrivacyNoticeCard`, `ModelCard`, `SelectModelButton`, `StatusBadge`, `InsightPreviewCards`, `InsightPreviewCard`, `InsightResultCard`, `AlertCard` (orphaned), `modelPathLabel`
- ViewModel functions removed: `selectModel`, `updateModelPath`, `saveHfToken`, `downloadModel`, `trackDownloadProgress`, `runAiInsights`, plus `ModelsUiState`, `AiInsightUiState`, `ModelDownloadProgress` data classes
- README rewritten to drop AI references

APK shrank from ~60 MB to ~16 MB.

### 2. Dynamic comparisons replace hardcoded placeholders

Two static "vs <month>" pills were hardcoded to `7% vs Feb` / `RM 4.20 vs Mar`. Both are now real:

- **Breakdown's `BreakdownTotalCard`** — `(curr - prev) / prev * 100`, sign-aware (`+12%` / `-8%`), TrendingUp/TrendingDown icon flips with direction. Hidden entirely when current OR previous total is zero. Per-period prev label: yesterday / `<prevMonth>` / `<prevYear>`.
- **Dashboard's `DailyAverageTile`** — actual current-month-daily-avg vs previous-month-daily-avg. Same hide-when-no-prev-data behavior. Green tint for spending-down, AppError tint for spending-up.

`BreakdownScreen` now computes `previousPeriodTotalCents` and passes it down.

### 3. New time-series bar chart on Breakdown

Added `SpendingTrendCard` between the total card and the donut. Renders:
- **Month period:** one bar per day of the selected month, sparse x-axis labels (every 7 days)
- **Year period:** one bar per month, all 12 month abbreviations labeled
- **Today period:** card not rendered (no meaningful time-series for one day)
- Highest bar gets coral fill; other non-zero bars use plum at 0.7 alpha; empty buckets get `AppSurfaceContainer` flat track
- Header switches between "DAILY TREND" / "MONTHLY TREND"; right-side average is computed across non-zero buckets only

Helper data class `TrendBucket(label, valueCents)` is private to `ExpenseTrackerApp.kt`.

### 4. Earlier UI bug fixes (still in place)
- Bottom nav `Text` has `maxLines = 1, softWrap = false, overflow = Visible` (prevents `Transactions` wrapping)
- Color tokens swept: `AppSWPrimary` for accent text/numbers (coral in dark), `AppOnSurface` for typed input text, `OnCoral` for content on coral surfaces
- `CoralContainer` (vivid `#FF987A`) used for amount badges, `OnCoral` for text on it

### 5. Dashboard and Transactions screens were split out

- Added `ui/DashboardScreen.kt` for the Home tab.
- Added `ui/TransactionsScreen.kt` for the Transactions tab and its icon action helper.
- `ExpenseTrackerApp.kt` now holds navigation plus the remaining Breakdown/Add Expense screens and shared composables.
- Verified with `:app:assembleDebug` and `:app:testDebugUnitTest` on 2026-05-07 using Android Studio JBR 21.

## Theme tokens cheat sheet

In `ui/theme/Theme.kt` the `SpendWiseColors` data class exposes per-mode tokens. Most-used:

| Token | Light | Dark | Use |
|---|---|---|---|
| `AppSWPrimary` | `#270F2E` deep plum | `#FF987A` vivid coral | Headlines, numeric displays — the brand "primary" |
| `AppCtaBg` | `#3D2444` | `#FF987A` | Primary button backgrounds |
| `AppCtaFg` | `#FFFFFF` | `#270F2E` | Text/icon on `AppCtaBg` |
| `CoralContainer` | `#FF987A` | `#FF987A` | Coral surfaces (same both modes) |
| `OnCoral` | `#270F2E` | `#270F2E` | Text/icon on `CoralContainer` |
| `PlumContainer` | `#3D2444` | `#3D2444` | Dark plum surfaces |
| `AppSurface` | `#FBF8FC` | `#1F1825` | Card backgrounds |
| `AppOnSurface` | `#1B1B1E` | `#F4ECF6` | Body text |
| `AppOnSurfaceVariant` | (mid-tone) | (mid-tone) | Secondary text |
| `AppOutline` | (light gray) | (mid gray) | Tertiary text, outlines |

**Critical distinction:** `PlumPrimary` exists but **flips to lavender** in dark mode (`#F1D9F7`). Use it sparingly. For "primary brand color" always reach for `AppSWPrimary` instead.

## Build / run

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\ameer\AppData\Local\Android\Sdk'
.\gradlew.bat :app:assembleDebug         # build APK
.\gradlew.bat :app:testDebugUnitTest     # run unit tests
.\gradlew.bat :app:installDebug          # install on connected device/emulator
```

Force-stop + relaunch when you reinstall (Compose state persists otherwise):

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am force-stop com.spendwise.app
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.spendwise.app/.MainActivity
```

## Verification checklist after any change

1. `assembleDebug` — green
2. `testDebugUnitTest` — green (suite covers `MoneyFormatter`, `ExpenseValidator`, `SpendingAnalyzer`)
3. Cold launch on device — Room migration runs cleanly (v3→v4 already shipped)
4. Bottom nav shows exactly **3 tabs**, all labels on one line
5. Add an expense → it appears on Home recent, Transactions list, Breakdown total + donut + trend chart
6. Toggle dark/light — every accent/title flips correctly (coral in dark, plum in light)

## Known good states

- `:app:assembleDebug` — last verified BUILD SUCCESSFUL on 2026-05-07 after screen split
- `:app:testDebugUnitTest` — all green on 2026-05-07 after screen split
- Tested on **Pixel_9_Pro AVD running Android 15 (emulator-5554)**
- Today's date during testing: **2026-05-07** (`Asia/Kuala_Lumpur` zone hardcoded in VM and BreakdownScreen)

## User preferences (sticky)

- **No emoji in UI strings** unless explicitly asked
- **No static placeholders** anywhere — if there's no data, hide the element or show a real empty state
- **Match the SpendWise.html design** for any new UI, especially color tokens
- **3 tabs only** — Home / Transactions / Breakdown. Don't add an Insights or Settings tab without asking
- Respond to "mcp" by using the Android MCP tools to drive the connected emulator
- Build before claiming work is done — pure code review without `assembleDebug` is not enough
- After UI changes, install/launch on the emulator and capture a screenshot before calling the work done

## Open / quick wins

These are in priority order. None are urgent.

1. **Continue splitting `ExpenseTrackerApp.kt`** — Dashboard and Transactions now have their own files. Remaining high-value split: move Breakdown and AddExpense into screen files, then shared composables (`PremiumSurface`, `HeaderBlock`, `SectionLabel`, `EmptyState`, `EmptyInnerState`, `TransactionRow`, `CategoryIcon`, `categoryAccent`, `categoryIconVector`) into a `ui/components/` package. Helper functions (`screenOrder`, `dateValue` extension on Expense) should eventually move into `ui/util.kt`.
2. **`SpendingAnalyzer` is currently bypassed in `BreakdownScreen`** — `BreakdownScreen` rebuilds `MonthlySpendingSummary` inline from raw expenses every recomposition. The pure analyzer in `analytics/` is only used for the dashboard. Either route Breakdown through the analyzer or move the breakdown logic into a new analyzer to keep the UI free of business logic.
3. **Currency is hardcoded MYR** — `MoneyFormatter` always emits `"RM "`. If you ever want to support other currencies, extract this. Right now do not bother.
4. **No edit-expense smoke test** — manual only. The Add/Edit screen reuses one composable (`AddExpenseScreen`) keyed off `existingExpense`. Worth adding an instrumentation test if/when you add Espresso.
5. **Year view trend chart hides empty months gracefully** but if a year has zero expenses the chart shows the empty state — could add a "Switch to month with data" affordance. Tiny polish only.
6. **Top Spend list (`TopSpendList`) on Breakdown** is filtered to `summary.recentTransactions.take(8)` — that's "recent" not "biggest." If the user actually wants top by amount, change the sort. Confirm with user before flipping.
7. **`SpendingTrendCard` chart bars use `Modifier.fillMaxHeight(ratio)` inside a Row with `Alignment.Bottom`** — works but the min-height (`0.02f`) creates a visible thin track for zero-value buckets. If user prefers truly invisible empty bars, drop the coerce.

## Things explicitly NOT to do

- **Don't reintroduce AI / LLM features** — user explicitly killed it. The codebase no longer has the dependency or the abstractions. If reintroducing later, it would require a fresh design pass.
- **Don't add an "Insights" or "Models" tab** — user picked 3 tabs.
- **Don't add cloud sync, accounts, auth, or push notifications** — local-first is the explicit positioning.
- **Don't use `PlumPrimary` for primary headlines/numbers in dark mode** — it goes lavender. Use `AppSWPrimary`.
- **Don't add emojis to UI strings** unless the user asks.
- **Don't `--amend` commits or skip pre-commit hooks.**

## Glossary of one-off names

- **SpendWise** — product name. App displays "SpendWise" on Dashboard header.
- **Plum & Coral** — the design system. Plum is the deep purple `#270F2E`, Coral is `#FF987A`.
- **`Asia/Kuala_Lumpur`** — hardcoded zone for date arithmetic. Don't change without asking; the user is in Malaysia.
- **`MYR` / `RM`** — Malaysian Ringgit. Hardcoded everywhere.
- **`AppContainer`** — manual DI bag, not Hilt or Koin.
- **`PremiumSurface`** — the rounded card wrapper used on most Breakdown/Insights cards. 24dp radius, `AppSurface` background.

## When stuck

- The plan file from this session is at `C:\Users\ameer\.claude\plans\playful-growing-sundae.md` — has the AI-removal plan with file-by-file edits.
- Original transcript with full context: `C:\Users\ameer\.claude\projects\C--Users-ameer-OneDrive-Documents-AmeerPersonalProject-App-Expense-Tracker\0d0978b4-9fcd-4ae2-8581-8b555b1e68dc.jsonl`
- Design HTML/JSX live at `C:\Users\ameer\Downloads\Expense App-handoff\expense-app\project\` — start with `SpendWise.html` to get the layout, then `screens.jsx` for component-level details.
