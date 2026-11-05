package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val reportEmailKey = stringPreferencesKey("report_email")

    val reportEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[reportEmailKey]
        }

    suspend fun saveReportEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[reportEmailKey] = email
        }
    }

    companion object {
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}
