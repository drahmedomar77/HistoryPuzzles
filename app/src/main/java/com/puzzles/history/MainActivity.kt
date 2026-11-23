package com.puzzles.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isAdsInitialized by mutableStateOf(false)

        try {
            MobileAds.initialize(this) {
                isAdsInitialized = true
                AdMobManager.loadInterstitial(this)
                AdMobManager.loadRewarded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val progress = Progress(this)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    AppRoot(progress = progress, isAdsReady = isAdsInitialized)
                }
            }
        }
    }
}

@Composable
fun AppRoot(progress: Progress, isAdsReady: Boolean) {
    var playingCategory by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    val category = playingCategory
    if (category != null) {
        PuzzleGameScreen(
            progress = progress,
            category = category,
            isAdsReady = isAdsReady,
            onExit = { playingCategory = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(NavyTop, NavyBottom)))
        ) {
            when (tab) {
                0 -> HomeScreen(progress = progress, onPlay = { playingCategory = it })
                1 -> AchievementsScreen(progress = progress)
                else -> SettingsScreen(progress = progress)
            }
        }
        if (isAdsReady) {
            BannerAdView(isAdsReady = isAdsReady, adInitError = null)
        }
        NavigationBar {
            val items = listOf("🏠" to "الرئيسية", "🏆" to "إنجازاتي", "⚙️" to "الإعدادات")
            items.forEachIndexed { i, item ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Text(item.first) },
                    label = { Text(item.second) }
                )
            }
        }
    }
}
