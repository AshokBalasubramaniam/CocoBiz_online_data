package com.cocobiz.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocobiz.app.data.local.AuthPreferences
import com.cocobiz.app.data.sync.CloudSyncManager
import com.cocobiz.app.domain.model.DarkModeOption
import com.cocobiz.app.ui.navigation.CocoBizNavGraph
import com.cocobiz.app.ui.theme.CocoBizTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var authPreferences: AuthPreferences
    @Inject lateinit var cloudSync: CloudSyncManager
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied, either way we proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val isLoggedIn = authPreferences.hasValidSession
        if (isLoggedIn) {
            // Pick up changes made on other devices (or upload pending ones from this device).
            lifecycleScope.launch {
                try {
                    cloudSync.refresh()
                } catch (e: HttpException) {
                    if (e.code() == 401) authPreferences.endSession()
                } catch (e: Exception) {
                    // Offline: keep showing cached data; pending uploads retry via WorkManager.
                }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            val darkTheme = when (settings.darkMode) {
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
            }

            CocoBizTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CocoBizNavGraph(isLoggedIn = isLoggedIn)
                    }
                }
            }
        }
    }
}
