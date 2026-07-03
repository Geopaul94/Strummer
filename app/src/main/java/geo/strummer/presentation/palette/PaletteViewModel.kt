package geo.strummer.presentation.palette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.data.palette.PaletteRepository
import geo.strummer.domain.guitar.ChordLibrary
import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.ChordType
import geo.strummer.domain.guitar.model.NoteName
import geo.strummer.domain.guitar.model.SavedPalette
import geo.strummer.player.GuitarSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaletteViewModel @Inject constructor(
    private val repository: PaletteRepository,
    private val session: GuitarSession,
) : ViewModel() {

    // The palette currently being edited (starts from whatever the session holds).
    private val _building = MutableStateFlow(session.palette.value)
    val building: StateFlow<List<Chord>> = _building.asStateFlow()

    private val _selectedRoot = MutableStateFlow(NoteName.C)
    val selectedRoot: StateFlow<NoteName> = _selectedRoot.asStateFlow()

    val savedPalettes: StateFlow<List<SavedPalette>> =
        repository.observePalettes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roots: List<NoteName> = NoteName.entries
    val types: List<ChordType> = ChordType.entries

    fun selectRoot(root: NoteName) { _selectedRoot.value = root }

    // Add a chord of the selected root + given type, if the library has a voicing.
    fun addChord(type: ChordType) {
        val chord = ChordLibrary.getChord(_selectedRoot.value, type) ?: return
        if (_building.value.any { it.displayName == chord.displayName }) return
        _building.value = _building.value + chord
        applyToSession()
    }

    fun removeChord(index: Int) {
        _building.value = _building.value.toMutableList().also {
            if (index in it.indices) it.removeAt(index)
        }
        applyToSession()
    }

    fun previewChord(chord: Chord) {
        // Strum the chord once so the user hears what they're adding.
        session.setPalette(_building.value)
        val idx = _building.value.indexOfFirst { it.displayName == chord.displayName }
        if (idx >= 0) {
            session.selectChord(idx)
            session.strum(geo.strummer.domain.guitar.model.StrumDirection.DOWN, 1500f)
        }
    }

    fun save(name: String) {
        val chords = _building.value
        if (chords.isEmpty()) return
        val finalName = name.ifBlank { "My Palette" }
        viewModelScope.launch { repository.save(finalName, chords) }
    }

    fun load(palette: SavedPalette) {
        _building.value = palette.chords
        applyToSession()
    }

    fun delete(palette: SavedPalette) {
        viewModelScope.launch { repository.delete(palette) }
    }

    // Push the working palette to the session so the Strum/Patterns screens use it.
    private fun applyToSession() {
        if (_building.value.isNotEmpty()) session.setPalette(_building.value)
    }
}
