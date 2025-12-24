package com.example.duonote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qr_preferences")

class QRDataStore(private val context: Context) {

    companion object {
        private val QR_CONTENT_KEY = stringPreferencesKey("qr_content")
        private val QR_TIMESTAMP_KEY = stringPreferencesKey("qr_timestamp")
    }

    suspend fun saveQRContent(content: String) {
        context.dataStore.edit { preferences ->
            preferences[QR_CONTENT_KEY] = content
            preferences[QR_TIMESTAMP_KEY] = System.currentTimeMillis().toString()
        }
    }

    val qrContent: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[QR_CONTENT_KEY]
    }

    val qrTimestamp: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[QR_TIMESTAMP_KEY]
    }

    suspend fun clearQRContent() {
        context.dataStore.edit { preferences ->
            preferences.remove(QR_CONTENT_KEY)
            preferences.remove(QR_TIMESTAMP_KEY)
        }
    }
}
