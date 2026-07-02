package geo.strummer.presentation.strum

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.audio.GuitarEngine
import geo.strummer.domain.guitar.model.GuitarString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class Phase0ViewModel @Inject constructor(
    private val engine: GuitarEngine,
) : ViewModel() {

    private val _diagnostics = MutableStateFlow(AudioDiagnostics.UNAVAILABLE)
    val diagnostics: StateFlow<AudioDiagnostics> = _diagnostics

    init {
        engine.start()
    }

    fun pluckString(guitarString: GuitarString, velocity: Float = 0.8f) {
        engine.pluckString(guitarString.index, guitarString.openMidiPitch, velocity)
        refreshDiagnostics()
    }

    fun muteString(guitarString: GuitarString) {
        engine.muteString(guitarString.index)
    }

    fun muteAll() {
        engine.muteAll()
    }

    fun refreshDiagnostics() {
        _diagnostics.value = engine.diagnostics()
    }

    override fun onCleared() {
        engine.stop()
        engine.release()
        super.onCleared()
    }
}
