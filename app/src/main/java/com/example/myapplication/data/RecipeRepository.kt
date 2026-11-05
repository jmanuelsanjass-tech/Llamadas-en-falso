package com.example.myapplication.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecipeRepository {
    private val _recipes = MutableStateFlow<List<CallRecipe>>(
        listOf(
            CallRecipe(
                id = "1",
                name = "Filtro Agresivo",
                description = "Bloquea todas las llamadas de números no verificados.",
                icon = Icons.Default.GppBad,
                isEnabled = true
            ),
            CallRecipe(
                id = "2",
                name = "Modo Confianza",
                description = "Solo permite llamadas de contactos.",
                icon = Icons.Default.Schedule,
                isEnabled = false
            ),
            CallRecipe(
                id = "3",
                name = "Protección IA",
                description = "Usa aprendizaje automático para identificar patrones de spam.",
                icon = Icons.Default.AutoAwesome,
                isEnabled = true
            )
        )
    )
    val recipes: StateFlow<List<CallRecipe>> = _recipes.asStateFlow()

    fun isRecipeEnabled(id: String): Boolean {
        return _recipes.value.find { it.id == id }?.isEnabled ?: false
    }

    fun toggleRecipe(id: String) {
        _recipes.value = _recipes.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }
}
