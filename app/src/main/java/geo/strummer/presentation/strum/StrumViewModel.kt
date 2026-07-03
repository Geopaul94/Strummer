package geo.strummer.presentation.strum

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.guitar.model.StrumDirection
import geo.strummer.player.GuitarSession
import javax.inject.Inject

// Thin ViewModel — all state and audio coordination live in the shared
// GuitarSession, so this just forwards UI events and re-exposes the flows the
// strum screen needs.
@HiltViewModel
class StrumViewModel @Inject constructor(
    val session: GuitarSession,
) : ViewModel() {

    val palette = session.palette
    val selectedChordIndex = session.selectedChordIndex
    val diagnostics = session.diagnostics
    val capo = session.capo
    val palmMute = session.palmMute

    fun selectChord(index: Int) = session.selectChord(index)
    fun strum(direction: StrumDirection, swipeSpeed: Float) = session.strum(direction, swipeSpeed)
    fun pickString(stringIndex: Int) = session.pickString(stringIndex)
    fun muteAll() = session.muteAll()
}
