package com.zords.soramusic

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.zords.soramusic.data.listentogether.JamInviteLink
import com.zords.soramusic.data.listentogether.consume
import com.zords.soramusic.data.settings.AppSettings
import com.zords.soramusic.playback.MusicLink
import com.zords.soramusic.playback.PlayerDeepLink
import com.zords.soramusic.playback.rememberMediaController
import com.zords.soramusic.playback.rememberPlayerState
import com.zords.soramusic.ui.MainViewModel
import com.zords.soramusic.ui.theme.SoraTheme
import com.zords.soramusic.ui.tv.TvApp
import com.zords.soramusic.ui.tv.display.TvRefreshRateController
import com.zords.soramusic.ui.tv.theme.SoraTvTheme

/**
 * Dedicated TV Activity for Android TV, Google TV, and Fire TV devices.
 * Launched via `android.intent.category.LEANBACK_LAUNCHER`.
 */
class TvActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach TV Refresh Rate and Display Controller
        TvRefreshRateController.attach(this)

        // Make window full edge-to-edge for 1080p/4K television screens
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // A cold launch from the Now Playing card (or a link) arrives here, not
        // through onNewIntent.
        if (savedInstanceState == null) consumeLinks(intent)

        setContent {
            SoraTvTheme {
                // The phone's Material theme too, so the components shared with
                // it (lyrics, mood sleeves) are set in the same face on its type scale.
                val systemFont by AppSettings.systemFont.collectAsState()
                SoraTheme(darkTheme = true, systemFont = systemFont) {
                    val mediaController = rememberMediaController()
                    val playerState = rememberPlayerState(mediaController)

                    TvApp(
                        viewModel = viewModel,
                        mediaController = mediaController,
                        playerState = playerState,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLinks(intent)
    }

    private fun consumeLinks(intent: Intent) {
        PlayerDeepLink.consume(intent)
        JamInviteLink.consume(intent)
        MusicLink.consume(intent)
    }

    override fun onDestroy() {
        // Closing the app (back out of Home, or the task being swiped away)
        // pauses the music rather than leaving it playing behind the launcher.
        if (isFinishing) {
            runCatching {
                val future = androidx.media3.session.MediaController.Builder(
                    applicationContext,
                    androidx.media3.session.SessionToken(
                        applicationContext,
                        android.content.ComponentName(
                            applicationContext,
                            com.zords.soramusic.playback.PlaybackService::class.java,
                        ),
                    ),
                ).buildAsync()
                future.addListener({
                    runCatching { future.get().run { pause(); release() } }
                }, androidx.core.content.ContextCompat.getMainExecutor(applicationContext))
            }
        }
        super.onDestroy()
        TvRefreshRateController.detach()
    }
}
