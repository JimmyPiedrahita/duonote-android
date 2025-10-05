package com.example.harmoninote

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

    // Guardar contenido del QR
    suspend fun saveQRContent(content: String) {
        context.dataStore.edit { preferences ->
            preferences[QR_CONTENT_KEY] = content
            preferences[QR_TIMESTAMP_KEY] = System.currentTimeMillis().toString()
        }
    }

    // Obtener contenido del QR como Flow
    val qrContent: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[QR_CONTENT_KEY]
    }

    // Obtener timestamp del último escaneo
    val qrTimestamp: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[QR_TIMESTAMP_KEY]
    }

    // Limpiar el contenido guardado
    suspend fun clearQRContent() {
        context.dataStore.edit { preferences ->
            preferences.remove(QR_CONTENT_KEY)
            preferences.remove(QR_TIMESTAMP_KEY)
        }
    }
}

