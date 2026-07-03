package geo.strummer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "strummer_prefs")

@Singleton
class OnboardingPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onboardedKey = booleanPreferencesKey("onboarded")

    val isOnboarded: Flow<Boolean> =
        context.dataStore.data.map { it[onboardedKey] ?: false }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[onboardedKey] = true }
    }
}
