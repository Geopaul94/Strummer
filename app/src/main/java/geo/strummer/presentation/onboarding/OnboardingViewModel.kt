package geo.strummer.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.data.prefs.OnboardingPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: OnboardingPrefs,
) : ViewModel() {

    // null = still loading; true/false once known (avoids a flash of onboarding).
    val isOnboarded: StateFlow<Boolean?> =
        prefs.isOnboarded.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun finishOnboarding() {
        viewModelScope.launch { prefs.setOnboarded() }
    }
}
