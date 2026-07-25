package com.example.alphamusic.core.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen
    @Serializable
    data object Search : Screen
    @Serializable
    data object Library : Screen
    @Serializable
    data object Settings : Screen
}
