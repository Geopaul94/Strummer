package geo.strummer.presentation.shell

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.player.GuitarSession
import javax.inject.Inject

// Owns the engine lifecycle for the whole app. Scoped to the activity's
// ViewModelStore, so start() runs once when the app UI is created and shutdown()
// runs when the activity is finished — no per-screen ViewModel touches the engine.
@HiltViewModel
class ShellViewModel @Inject constructor(
    val session: GuitarSession,
) : ViewModel() {

    init {
        session.start()
    }

    override fun onCleared() {
        session.shutdown()
        super.onCleared()
    }
}
