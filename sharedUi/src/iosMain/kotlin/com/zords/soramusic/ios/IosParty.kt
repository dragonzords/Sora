package com.zords.soramusic.ios

import com.zords.soramusic.data.listentogether.JamInvite
import com.zords.soramusic.data.listentogether.JamInviteLink
import com.zords.soramusic.data.listentogether.ListenTogether
import com.zords.soramusic.data.listentogether.PartyAccount
import com.zords.soramusic.data.listentogether.PartyHost
import com.zords.soramusic.platform.ktorClient
import com.zords.soramusic.ui.player.PartyUi
import com.zords.soramusic.ui.screens.PartyInfo
import com.russhwolf.settings.NSUserDefaultsSettings
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.cinterop.convert
import platform.Foundation.NSUserDefaults
import platform.posix.CLOCK_MONOTONIC_RAW
import platform.posix.clock_gettime_nsec_np

/** Listen Together on iOS: the host [ListenTogether] runs on, and the party as the shared UI reads it. */
internal object IosParty {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val ui: StateFlow<PartyUi> = ListenTogether.state
        .map { PartyUi(it.inParty, it.controlsLocked, it.members, it.you, it.code) }
        .stateIn(scope, SharingStarted.Eagerly, PartyUi())

    val info: StateFlow<PartyInfo> = ListenTogether.state
        .map { PartyInfo(it.code, it.members.size) }
        .stateIn(scope, SharingStarted.Eagerly, PartyInfo(null, 0))

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun install() {
        ListenTogether.init(
            PartyHost(
                settings = NSUserDefaultsSettings(NSUserDefaults(suiteName = "sora_listen_together")),
                defaultServer = JamInvite.ORIGIN,
                // Counts through sleep, as Android's elapsedRealtime: TimeSource.Monotonic stops while the phone sleeps, so
                // after a locked spell the party clock read minutes behind and every scheduled start was minutes away.
                elapsedRealtimeMs = { (clock_gettime_nsec_np(CLOCK_MONOTONIC_RAW.convert()) / 1_000_000uL).toLong() },
                account = {
                    IosAuth.account.value?.takeIf { IosAuth.signedIn.value }?.let {
                        PartyAccount(
                            idSeed = it.email.ifBlank { it.name },
                            names = listOf(it.name, it.email.substringBefore('@')),
                            avatar = it.thumbnailUrl,
                        )
                    }
                },
                createClient = { config -> ktorClient(config) },
                isTransportError = { it is DarwinHttpRequestException },
            ),
        )
    }
}

/** Called by Swift with a `sora://party/...` or `https://.../invite/...` link; false when it is neither. */
fun handleInviteUrl(url: String): Boolean = JamInviteLink.offer(url)
