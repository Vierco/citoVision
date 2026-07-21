package dev.lovelace.citovision.infrastructure.persistence.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Claves de DataStore Preferences centralizadas (ver Skill datastore-multiplatform). */
object AppPreferenceKeys {
    val GUEST_SESSION = booleanPreferencesKey("guest_session")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
}
