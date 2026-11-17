package com.example.borrowbox

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.borrowbox.data.local.BorrowBoxDatabase
import com.example.borrowbox.data.repository.AuthRepository
import com.example.borrowbox.navigation.BorrowBoxApp
import com.example.borrowbox.notifications.ReminderWorker
import com.example.borrowbox.ui.screens.AppLockScreen
import com.example.borrowbox.ui.theme.BorrowBoxTheme
import com.example.borrowbox.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderWorker.schedule(this)

        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = BorrowBoxDatabase.getInstance(applicationContext)
                    val authRepo = AuthRepository(db.userDao(), applicationContext)
                    authRepo.logout()
                } catch (e: Exception) {}
            }
        }

        setContent {
            val themeConfig by mainViewModel.themeConfig.collectAsState()
            val appLockEnabled by mainViewModel.appLockEnabled.collectAsState()
            val biometricEnabled by mainViewModel.biometricEnabled.collectAsState()
            val appPin by mainViewModel.appPin.collectAsState()
            val pinSalt by mainViewModel.pinSalt.collectAsState()
            
            var isUnlockedThisSession by rememberSaveable { mutableStateOf(false) }
            var hasCheckedInitialLock by rememberSaveable { mutableStateOf(false) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        isUnlockedThisSession = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(appLockEnabled, appPin) {
                if (appLockEnabled != null && !hasCheckedInitialLock) {
                    if (appLockEnabled == false || appPin == null) {
                        isUnlockedThisSession = true
                    }
                    hasCheckedInitialLock = true
                }
            }

            BorrowBoxTheme(themeConfig = themeConfig) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (appLockEnabled == null) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val shouldShowLock = appLockEnabled == true && appPin != null && !isUnlockedThisSession
                        
                        if (shouldShowLock) {
                            AppLockScreen(
                                onUnlock = { isUnlockedThisSession = true },
                                biometricEnabled = biometricEnabled,
                                correctPin = appPin,
                                pinSalt = pinSalt
                            )
                        } else {
                            BorrowBoxApp(mainViewModel)
                        }
                    }
                }
            }
        }
    }
}
