package geo.strummer.presentation.strum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.audio.GuitarEngine
import geo.strummer.domain.guitar.ChordLibrary
import geo.strummer.domain.guitar.StrumEngine
import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.StrumDirection
import geo.strummer.domain.guitar.model.StrumEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrumUiState(
    val palette: List<Chord> = emptyList(),
    val selectedChordIndex: Int = 0,
    val diagnostics: AudioDiagnostics = AudioDiagnostics.UNAVAILABLE,
    val capo: Int = 0,
) {
    val selectedChord: Chord? get() = palette.getOrNull(selectedChordIndex)
}

@HiltViewModel
class StrumViewModel @Inject constructor(
    private val engine: GuitarEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(StrumUiState())
    val state: StateFlow<StrumUiState> = _state

    init {
        engine.start()
        _state.value = _state.value.copy(palette = ChordLibrary.defaultPalette())
        refreshDiagnostics()
    }

    fun selectChord(index: Int) {
        _state.value = _state.value.copy(selectedChordIndex = index)
    }

    // Called by the strum gesture detector. Expands the held chord voicing into
    // timed per-string pluck events and schedules them with the correct rake delay.
    fun strum(direction: StrumDirection, swipeSpeed: Float) {
        val chord = _state.value.selectedChord ?: return
        val events = StrumEngine.strum(
            voicing = chord.voicing,
            direction = direction,
            swipeSpeed = swipeSpeed,
            capo = _state.value.capo,
        )
        scheduleStrumEvents(events)
    }

    // Tap a single string within the strum area.
    fun pickString(stringIndex: Int) {
        val chord = _state.value.selectedChord ?: return
        val event = StrumEngine.pick(
            voicing = chord.voicing,
            stringIndex = stringIndex,
            capo = _state.value.capo,
        ) ?: return
        engine.pluckString(event.stringIndex, event.midiPitch, event.velocity)
    }

    fun muteAll() {
        engine.muteAll()
    }

    fun refreshDiagnostics() {
        _state.value = _state.value.copy(diagnostics = engine.diagnostics())
    }

    // Schedule strum events with their rake delays. The first event fires
    // immediately; subsequent events fire after their delayMs offset.
    // Using coroutine delay for the rake timing — at 3-16ms per string, this is
    // well within coroutine scheduling precision and inaudibly close to "exact."
    // For sample-accurate timing (Phase 2+ patterns), we'd push timed events
    // into the native scheduler; for strum rake, coroutine delay is fine.
    private fun scheduleStrumEvents(events: List<StrumEvent>) {
        if (events.isEmpty()) return

        viewModelScope.launch {
            var lastDelay = 0L
            for (event in events) {
                val wait = event.delayMs - lastDelay
                if (wait > 0) delay(wait)
                engine.pluckString(event.stringIndex, event.midiPitch, event.velocity)
                lastDelay = event.delayMs
            }
            refreshDiagnostics()
        }
    }

    override fun onCleared() {
        engine.stop()
        engine.release()
        super.onCleared()
    }
}
