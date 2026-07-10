package com.spendwise.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.app.ui.ExpenseTrackerApp
import com.spendwise.app.ui.ExpenseTrackerViewModel
import com.spendwise.app.ui.ExpenseTrackerViewModelFactory
import com.spendwise.app.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    // Saved appearance preference, loaded asynchronously during the splash.
    // MutableState (not a plain var) so the composition re-reads it the moment
    // the preload lands — which happens behind the held splash, before the
    // first visible frame.
    private var preloadedDarkPreference: Boolean? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val appContainer = (application as ExpenseTrackerApplication).container

        // The first Compose frame must already be in the user's saved theme or
        // cold start shows a dark→light flash (StateFlow's initial value is
        // null → system-theme fallback). Previously this was solved with a
        // runBlocking DataStore read that stalled the main thread for up to
        // 250ms; now the system splash is simply held on screen until the
        // preference arrives, so the read runs in parallel with the rest of
        // startup and the main thread never blocks. The timeout keeps a
        // stalled DataStore from pinning the splash: past 250ms we proceed on
        // the system theme, same tradeoff as before.
        var startupPreferenceLoaded = false
        splashScreen.setKeepOnScreenCondition { !startupPreferenceLoaded }
        lifecycleScope.launch {
            preloadedDarkPreference = withTimeoutOrNull(250L) {
                appContainer.appearancePreferenceStore.startupDarkModePreference.first()
            }
            // The activity-window background is painted before Compose draws,
            // so it has to agree with the resolved preference or the window
            // briefly shows a mismatched colour underneath the content. XML
            // qualifiers (values-night) can't see the saved preference, only
            // the system setting — hence the programmatic override.
            val resolvedDarkAtStartup = preloadedDarkPreference ?: resources.configuration
                .let { (it.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES }
            window.setBackgroundDrawable(
                ColorDrawable(if (resolvedDarkAtStartup) 0xFF0F0D1A.toInt() else 0xFFF4F1FA.toInt())
            )
            startupPreferenceLoaded = true
        }

        setContent {
            val viewModel: ExpenseTrackerViewModel = viewModel(
                factory = ExpenseTrackerViewModelFactory(application, appContainer)
            )
            val systemDarkMode = isSystemInDarkTheme()
            val initialPreference = preloadedDarkPreference
            val startupPreference = viewModel.startupDarkModePreference
                .collectAsStateWithLifecycle()
                .value
            // Prefer the live StateFlow once it has loaded; until then fall
            // back to the value we preloaded synchronously, so the very first
            // composition already paints the user's saved theme. The system
            // fallback only kicks in when no preference has ever been saved.
            val isDarkMode = startupPreference ?: initialPreference ?: systemDarkMode
            SideEffect {
                // Edge-to-edge: content paints under the system bars, with the
                // bars showing a translucent tint of the app canvas. Re-invoked on
                // theme change so the scrim follows light/dark.
                val barLight = Color.argb(0xCC, 247, 243, 236)
                val barDark = Color.argb(0xCC, 15, 13, 26)
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode)
                        SystemBarStyle.dark(barDark)
                    else
                        SystemBarStyle.light(barLight, barDark),
                    navigationBarStyle = if (isDarkMode)
                        SystemBarStyle.dark(barDark)
                    else
                        SystemBarStyle.light(barLight, barDark)
                )
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkMode
                    isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            ExpenseTrackerTheme(darkTheme = isDarkMode) {
                ExpenseTrackerApp(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onDarkModeChange = viewModel::setDarkMode
                )
            }
        }
    }
}
