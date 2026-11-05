package com.example.myapplication.data

import androidx.compose.ui.graphics.vector.ImageVector

data class CallRecipe(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = false
)
