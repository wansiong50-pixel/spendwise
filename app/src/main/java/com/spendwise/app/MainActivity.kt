package com.spendwise.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.app.ui.ExpenseTrackerViewModel
import com.spendwise.app.ui.ExpenseTrackerViewModelFactory
import com.spendwise.app.ui.botanical.BotanicalTheme
import com.spendwise.app.ui.botanical.ExpenseTrackerApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Edge-to-edge with clear bars: the painted hero and the navy pages
        // run under the status bar. Icon colours follow the screen (see the
        // shell) — light over the dark tops, dark over Home's pale paper.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val appContainer = (application as ExpenseTrackerApplication).container

        setContent {
            val viewModel: ExpenseTrackerViewModel = viewModel(
                factory = ExpenseTrackerViewModelFactory(application, appContainer)
            )
            BotanicalTheme {
                ExpenseTrackerApp(viewModel = viewModel)
            }
        }
    }
}
