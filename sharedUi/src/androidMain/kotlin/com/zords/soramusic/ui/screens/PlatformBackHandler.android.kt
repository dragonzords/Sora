package com.zords.soramusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
