# Dark Mode Design

## Goal

Add a user-controlled dark mode option to the SpendWise Android app. The first implementation will use a quick dashboard header toggle because the app does not currently have a dedicated Settings screen and the option should be easy to find.

## User Experience

The dashboard header will show an icon-only moon/sun toggle aligned with the existing premium visual style. Tapping it switches between light and dark mode immediately. The selected mode persists across app restarts.

The toggle applies to the whole app, including the dashboard, transaction list, breakdown, insights, model management, add/edit expense flow, bottom navigation, cards, input surfaces, badges, and empty/error states.

## Architecture

Theme preference will live in a new DataStore-backed preference store, separate from the existing AI model preference store. `AppContainer` will create this store and pass it into `ExpenseTrackerViewModel`.

`ExpenseTrackerViewModel` will expose:

- `isDarkMode: StateFlow<Boolean>`
- `setDarkMode(enabled: Boolean)`

`MainActivity` will collect `isDarkMode` and pass it to `ExpenseTrackerTheme(darkTheme = isDarkMode)`.

## Theme System

`Theme.kt` will gain a dark Material color scheme and a theme-aware app color token object exposed through composition. Existing custom constants such as `AppBackground`, `AppSurface`, `PlumPrimary`, and related aliases are currently fixed light values, so dark mode needs a small token refactor. UI code should read dynamic theme tokens instead of importing fixed colors for surfaces and text.

Accent colors will remain recognizable: plum and coral stay the brand anchors, but their dark-mode versions will be adjusted for contrast on dark surfaces.

## Components

The dashboard header will accept:

- `isDarkMode: Boolean`
- `onDarkModeChange: (Boolean) -> Unit`

The toggle will use Material iconography and a compact touch target consistent with the current header controls.

## Error Handling

If the preference has no saved value, the app defaults to light mode to preserve current behavior. DataStore read/write errors are not expected in normal use; the flow can fall back to light mode if an exception occurs.

## Testing

Unit tests will cover a small pure Kotlin preference mapping helper so default light mode and persisted dark-mode values are exercised without needing Android framework storage in unit tests. The DataStore wrapper will remain thin and delegate conversion to that helper.

Build verification will run `testDebugUnitTest` and `assembleDebug` after implementation.
