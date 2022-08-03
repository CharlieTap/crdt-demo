package com.tap.contacts.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.michaelbull.result.get
import com.tap.hlc.HybridLogicalClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactsDatastore(private val datastore: DataStore<Preferences>) {

    companion object {
        val LOCAL_CLOCK         = stringPreferencesKey(name = "local_clock")
        val LAST_SYNC_ID        = longPreferencesKey(name = "last_sync_id")
    }

    val localClock: Flow<HybridLogicalClock> = datastore.data
        .map { preferences ->
            preferences[LOCAL_CLOCK]?.let {
                HybridLogicalClock.decodeFromString(it).get()
        } ?: HybridLogicalClock()
    }

    suspend fun saveLocalClock(localClock: HybridLogicalClock) {
        datastore.edit { preferences ->
            preferences[LOCAL_CLOCK] = HybridLogicalClock.encodeToString(localClock)
        }
    }

    val lastSyncId: Flow<Long> = datastore.data
        .map { preferences ->
            preferences[LAST_SYNC_ID] ?: 0
    }

    suspend fun saveLastSyncId(lastSyncId: Long) {
        datastore.edit { preferences ->
            preferences[LAST_SYNC_ID] = lastSyncId
        }
    }

}