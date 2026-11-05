package com.example.myapplication

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

sealed interface Destination : NavKey {
    @Serializable
    data object Home : Destination

    @Serializable
    data object CallLog : Destination

    @Serializable
    data object RecipeHub : Destination
}
