package io.github.arashiyama11.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.arashiyama11.domain.repository.ISettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single(binds = [ISettingsRepository::class])
class SettingsRepository(private val context: Context) : ISettingsRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    override val arrayOriginIndex: StateFlow<Int> =
        context.dataStore.data.map { it[PreferencesKeys.LIST_FIRST_INDEX] ?: 0 }
            .stateIn(scope, SharingStarted.Lazily, 0)

    override fun setListFirstIndex(index: Int) {
        println("set $index")
        scope.launch {
            context.dataStore.edit { settings ->
                settings[PreferencesKeys.LIST_FIRST_INDEX] = index
            }
        }
    }

    init {
        scope.launch {
            context.dataStore.data.collect {
                val index = it[PreferencesKeys.LIST_FIRST_INDEX]
                Log.d("Settings", index.toString())
            }
        }
    }


    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        object PreferencesKeys {
            val LIST_FIRST_INDEX = intPreferencesKey("selected_file_id")
        }
    }
}