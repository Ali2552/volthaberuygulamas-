package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.config.SourceConfig
import com.example.data.model.MgmLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_SHOW_DEBUG_TAGS = booleanPreferencesKey("show_debug_tags")

        // MGM Konum Tercihleri
        val KEY_SELECTED_IL = stringPreferencesKey("mgm_selected_il")
        val KEY_SELECTED_ILCE = stringPreferencesKey("mgm_selected_ilce")
        val KEY_SELECTED_MERKEZ_ID = intPreferencesKey("mgm_selected_merkez_id")
        val KEY_SELECTED_IST_NO = intPreferencesKey("mgm_selected_ist_no")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val showDebugTagsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_DEBUG_TAGS] ?: true
    }

    val selectedLocationFlow: Flow<MgmLocation> = context.dataStore.data.map { preferences ->
        val il = preferences[KEY_SELECTED_IL] ?: SourceConfig.Weather.DEFAULT_IL
        val ilce = preferences[KEY_SELECTED_ILCE] ?: SourceConfig.Weather.DEFAULT_ILCE
        val merkezId = preferences[KEY_SELECTED_MERKEZ_ID] ?: SourceConfig.Weather.DEFAULT_MERKEZ_ID
        val istNo = preferences[KEY_SELECTED_IST_NO] ?: SourceConfig.Weather.DEFAULT_IST_NO
        MgmLocation(il = il, ilce = ilce, merkezId = merkezId, istNo = istNo)
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setShowDebugTags(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_DEBUG_TAGS] = show
        }
    }

    suspend fun saveSelectedLocation(location: MgmLocation) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_IL] = location.il
            preferences[KEY_SELECTED_ILCE] = location.ilce
            preferences[KEY_SELECTED_MERKEZ_ID] = location.merkezId
            preferences[KEY_SELECTED_IST_NO] = location.istNo
        }
    }
}
